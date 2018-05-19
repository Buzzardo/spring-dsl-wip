/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.dsl.antlr.support;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.Vocabulary;
import org.antlr.v4.runtime.atn.ATN;
import org.antlr.v4.runtime.atn.ATNState;
import org.antlr.v4.runtime.atn.PredicateTransition;
import org.antlr.v4.runtime.atn.RuleStopState;
import org.antlr.v4.runtime.atn.RuleTransition;
import org.antlr.v4.runtime.atn.Transition;
import org.antlr.v4.runtime.misc.IntervalSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dsl.antlr.AntlrCompletionEngine;

/**
 * Default implementation of a {@link AntlrCompletionEngine}.
 * <p>
 * This is mostly based on a generic implementation from
 * https://github.com/mike-lischke/antlr4-c3 and has been modified to suit our
 * needs.
 *
 * @author Janne Valkealahti
 *
 */
public class DefaultAntlrCompletionEngine implements AntlrCompletionEngine {

	private static final Logger log = LoggerFactory.getLogger(DefaultAntlrCompletionEngine.class);

    public static class CandidatesCollection {
        public Map<Integer, List<Integer>> tokens = new HashMap<>();
        public Map<Integer, List<Integer>> rules = new HashMap<>();
    }

    public static class FollowSetWithPath {
        public IntervalSet intervals;
        public List<Integer> path;
        public List<Integer> following;
    }

    public static class FollowSetsHolder {
        public List<FollowSetWithPath> sets;
        public IntervalSet combined;
    }

    public static class PipelineEntry {
        ATNState state;
        Integer tokenIndex;
        public PipelineEntry(ATNState state, Integer tokenIndex) {
            this.state = state;
            this.tokenIndex = tokenIndex;
        }
    }

    private boolean showResult = true;
    private boolean showDebugOutput = true;
    private boolean debugOutputWithTransitions = true;
    private boolean showRuleStack = true;

    private Set<Integer> ignoredTokens = new HashSet<>();
    private Set<Integer> preferredRules = new HashSet<>();

    private Parser parser;
    private ATN atn;
    private Vocabulary vocabulary;
    private String[] ruleNames;
    private List<Token> tokens;

    private int tokenStartIndex = 0;
    private int statesProcessed = 0;

    private final Map<Integer, Map<Integer, Set<Integer>>> shortcutMap = new HashMap<>();
    private final CandidatesCollection candidates = new CandidatesCollection(); // The collected candidates (rules and tokens).
    private final static Map<String, Map<Integer, FollowSetsHolder>> followSetsByATN = new HashMap<>();

    public DefaultAntlrCompletionEngine(Parser parser) {
    	this.parser = parser;
    	this.atn = parser.getATN();
        this.vocabulary = parser.getVocabulary();
        this.ruleNames = parser.getRuleNames();
        if (ignoredTokens != null) {
            this.ignoredTokens = ignoredTokens;
        }
        if (preferredRules != null) {
            this.preferredRules = preferredRules;
        }
    }

    public CandidatesCollection collectCandidates(int caretTokenIndex, ParserRuleContext context) {
        this.shortcutMap.clear();
        this.candidates.rules.clear();
        this.candidates.tokens.clear();
        this.statesProcessed = 0;

        this.tokenStartIndex = context != null ? context.start.getTokenIndex() : 0;
        TokenStream tokenStream  = this.parser.getInputStream();

        int currentIndex = tokenStream.index();
        tokenStream.seek(this.tokenStartIndex);
        this.tokens = new LinkedList<>();
        int offset = 1;
        while (true) {
            Token token = tokenStream.LT(offset++);
            this.tokens.add(token);
            if (token.getTokenIndex() >= caretTokenIndex || token.getType() == Token.EOF) {
                break;
            }
        }
        tokenStream.seek(currentIndex);

        LinkedList<Integer> callStack = new LinkedList<>();
        int startRule = context != null ? context.getRuleIndex() : 0;
        this.processRule(this.atn.ruleToStartState[startRule], 0, callStack, "");

//        if (this.showResult) {
//            log.debug("States processed: " + this.statesProcessed);
//            log.debug("\n\nCollected rules:\n");
//            for (Entry<Integer, List<Integer>> rule : this.candidates.rules.entrySet()) {
//                String path = "";
//                for (let token of rule[1]) {
//                    path += this.ruleNames[token] + " ";
//                }
//                log.debug(this.ruleNames[rule[0]] + ", path: ", path);
//            }
//
//            let sortedTokens: Set<string> = new Set();
//            for (let token of this.candidates.tokens) {
//                let value: string = this.vocabulary.getDisplayName(token[0]);
//                for (let following of token[1])
//                    value += " " + this.vocabulary.getDisplayName(following);
//                sortedTokens.add(value);
//            }
//
//            log.debug("\n\nCollected tokens:\n");
//            for (let symbol of sortedTokens) {
//            	log.debug(symbol);
//            }
//            log.debug("\n\n");
//        }


        return this.candidates;
    }

    private boolean checkPredicate(PredicateTransition transition) {
        return transition.getPredicate().eval(this.parser, ParserRuleContext.EMPTY);
    }

    private boolean translateToRuleIndex(List<Integer> ruleStack) {
        if (this.preferredRules.isEmpty())
            return false;

        // Loop over the rule stack from highest to lowest rule level. This way we properly handle the higher rule
        // if it contains a lower one that is also a preferred rule.
        for (int i = 0; i < ruleStack.size(); ++i) {
            if (this.preferredRules.contains(ruleStack.get(i))) {
                // Add the rule to our candidates list along with the current rule path,
                // but only if there isn't already an entry like that.
                List<Integer> path = new LinkedList<>(ruleStack.subList(0, i));
                boolean addNew = true;
                for (Map.Entry<Integer, List<Integer>> entry : this.candidates.rules.entrySet()) {
                    if (!entry.getKey().equals(ruleStack.get(i)) || entry.getValue().size() != path.size()) {
                        continue;
                    }
                    // Found an entry for this rule. Same path? If so don't add a new (duplicate) entry.
                    if (path.equals(entry.getValue())) {
                        addNew = false;
                        break;
                    }
                }

                if (addNew) {
                    this.candidates.rules.put(ruleStack.get(i), path);
//                    if (showDebugOutput && logger.isLoggable(Level.FINE)) {
//                        logger.fine("=====> collected: " + this.ruleNames[i]);
//                    }
                }
                return true;
            }
        }

        return false;
    }

    private List<Integer> getFollowingTokens(Transition initialTransition) {
        LinkedList<Integer> result = new LinkedList<>();
        LinkedList<ATNState> seen = new LinkedList<>(); // unused but in orig
        LinkedList<ATNState> pipeline = new LinkedList<>();
        pipeline.add(initialTransition.target);

        while (!pipeline.isEmpty()) {
            ATNState state = pipeline.removeLast();

            for (Transition transition: state.getTransitions()) {
                if (transition.getSerializationType() == Transition.ATOM) {
                    if (!transition.isEpsilon()) {
                        List<Integer> list = transition.label().toList();
                        if (list.size() == 1 && !this.ignoredTokens.contains(list.get(0))) {
                            result.addLast(list.get(0));
                            pipeline.addLast(transition.target);
                        }
                    } else {
                        pipeline.addLast(transition.target);
                    }
                }
            }
        }

        return result;
    }

    private LinkedList<FollowSetWithPath> determineFollowSets(ATNState start, ATNState stop){
        LinkedList<FollowSetWithPath> result = new LinkedList<>();
        Set<ATNState> seen = new HashSet<>();
        LinkedList<Integer> ruleStack = new LinkedList<>();

        this.collectFollowSets(start, stop, result, seen, ruleStack);

        return result;
    }

    private void collectFollowSets(ATNState s, ATNState stopState, LinkedList<FollowSetWithPath> followSets,
            Set<ATNState> seen, LinkedList<Integer> ruleStack) {

        if (seen.contains(s))
            return;

        seen.add(s);

        if (s.equals(stopState) || s.getStateType() == ATNState.RULE_STOP) {
            FollowSetWithPath set = new FollowSetWithPath();
            set.intervals = IntervalSet.of(Token.EPSILON);
            set.path = new LinkedList<Integer>(ruleStack);
            followSets.addLast(set);
            return;
        }

        for (Transition transition : s.getTransitions()) {
            if (transition.getSerializationType() == Transition.RULE) {
                RuleTransition ruleTransition = (RuleTransition) transition;
                if (ruleStack.indexOf(ruleTransition.target.ruleIndex) != -1) {
                    continue;
                }
                ruleStack.addLast(ruleTransition.target.ruleIndex);
                this.collectFollowSets(transition.target, stopState, followSets, seen, ruleStack);
                ruleStack.removeLast();

            } else if (transition.getSerializationType() == Transition.PREDICATE) {
                if (this.checkPredicate((PredicateTransition) transition)) {
                    this.collectFollowSets(transition.target, stopState, followSets, seen, ruleStack);
                }
            } else if (transition.isEpsilon()) {
                this.collectFollowSets(transition.target, stopState, followSets, seen, ruleStack);
            } else if (transition.getSerializationType() == Transition.WILDCARD) {
                FollowSetWithPath set = new FollowSetWithPath();
                set.intervals = IntervalSet.of(Token.MIN_USER_TOKEN_TYPE, this.atn.maxTokenType);
                set.path = new LinkedList<Integer>(ruleStack);
                followSets.addLast(set);
            } else {
                IntervalSet label = transition.label();
                if (label != null && label.size() > 0) {
                    if (transition.getSerializationType() == Transition.NOT_SET) {
                        label = label.complement(IntervalSet.of(Token.MIN_USER_TOKEN_TYPE, this.atn.maxTokenType));
                    }
                    FollowSetWithPath set = new FollowSetWithPath();
                    set.intervals = label;
                    set.path = new LinkedList<Integer>(ruleStack);
                    set.following = this.getFollowingTokens(transition);
                    followSets.addLast(set);
                }
            }
        }
    }

    private Set<Integer> processRule(ATNState startState, int tokenIndex, LinkedList<Integer> callStack, String indentation) {

        // Start with rule specific handling before going into the ATN walk.

        // Check first if we've taken this path with the same input before.
        Map<Integer,Set<Integer>> positionMap = this.shortcutMap.get(startState.ruleIndex);
        if (positionMap == null) {
            positionMap = new HashMap<>();
            this.shortcutMap.put(startState.ruleIndex, positionMap);
        } else {
            if (positionMap.containsKey(tokenIndex)) {
//                if (showDebugOutput) {
//                    logger.fine("=====> shortcut");
//                }
                return positionMap.get(tokenIndex);
            }
        }

        Set<Integer> result = new HashSet<>();

        // For rule start states we determine and cache the follow set, which gives us 3 advantages:
        // 1) We can quickly check if a symbol would be matched when we follow that rule. We can so check in advance
        //    and can save us all the intermediate steps if there is no match.
        // 2) We'll have all symbols that are collectable already together when we are at the caret when entering a rule.
        // 3) We get this lookup for free with any 2nd or further visit of the same rule, which often happens
        //    in non trivial grammars, especially with (recursive) expressions and of course when invoking code completion
        //    multiple times.
        Map<Integer, FollowSetsHolder> setsPerState = followSetsByATN.get(this.parser.getClass().getName());
        if (setsPerState == null) {
            setsPerState = new HashMap<>();
            followSetsByATN.put(this.parser.getClass().getName(), setsPerState);
        }

        FollowSetsHolder followSets = setsPerState.get(startState.stateNumber);
        if (followSets == null) {
            followSets = new FollowSetsHolder();
            setsPerState.put(startState.stateNumber, followSets);
            RuleStopState stop = this.atn.ruleToStopState[startState.ruleIndex];
            followSets.sets = this.determineFollowSets(startState, stop);

            // Sets are split by path to allow translating them to preferred rules. But for quick hit tests
            // it is also useful to have a set with all symbols combined.
            IntervalSet combined = new IntervalSet();
            for (FollowSetWithPath set: followSets.sets) {
                combined.addAll(set.intervals);
            }
            followSets.combined = combined;
        }

        callStack.addLast(startState.ruleIndex);
        int currentSymbol = this.tokens.get(tokenIndex).getType();

        if (tokenIndex >= this.tokens.size() - 1) { // At caret?
            if (this.preferredRules.contains(startState.ruleIndex)) {
                // No need to go deeper when collecting entries and we reach a rule that we want to collect anyway.
                this.translateToRuleIndex(callStack);
            } else {
                // Convert all follow sets to either single symbols or their associated preferred rule and add
                // the result to our candidates list.
                for (FollowSetWithPath set: followSets.sets) {
                    LinkedList<Integer> fullPath = new LinkedList<>(callStack);
                    fullPath.addAll(set.path);
                    if (!this.translateToRuleIndex(fullPath)) {
                        for (int symbol : set.intervals.toList()) {
                            if (!this.ignoredTokens.contains(symbol)) {
//                                if (showDebugOutput && logger.isLoggable(Level.FINE)) {
//                                    logger.fine("=====> collected: " + this.vocabulary.getDisplayName(symbol));
//                                }
                                if (!this.candidates.tokens.containsKey(symbol))

                                    this.candidates.tokens.put(symbol, set.following); // Following is empty if there is more than one entry in the set.
                                else {
                                    // More than one following list for the same symbol.
                                    if (!this.candidates.tokens.get(symbol).equals(set.following)) { // XXX js uses !=
                                        this.candidates.tokens.put(symbol, new LinkedList<Integer>());
                                    }
                                }
                            } else {
//                                logger.fine("====> collection: Ignoring token: " + symbol);
                            }
                        }
                    }
                }
            }

            callStack.removeLast();
            return result;

        } else {
            // Process the rule if we either could pass it without consuming anything (epsilon transition)
            // or if the current input symbol will be matched somewhere after this entry point.
            // Otherwise stop here.
            if (!followSets.combined.contains(Token.EPSILON) && !followSets.combined.contains(currentSymbol)) {
                callStack.removeLast();
                return result;
            }
        }

        // The current state execution pipeline contains all yet-to-be-processed ATN states in this rule.
        // For each such state we store the token index + a list of rules that lead to it.
        LinkedList<PipelineEntry> statePipeline = new LinkedList<>();
        PipelineEntry currentEntry;

        // Bootstrap the pipeline.
        statePipeline.add(new PipelineEntry(startState, tokenIndex));

        while (!statePipeline.isEmpty()) {
            currentEntry = statePipeline.removeLast();
            ++this.statesProcessed;

            currentSymbol = this.tokens.get(currentEntry.tokenIndex).getType();

            boolean atCaret = currentEntry.tokenIndex >= this.tokens.size() - 1;
//            if (logger.isLoggable(Level.FINE)) {
//                printDescription(indentation, currentEntry.state, this.generateBaseDescription(currentEntry.state), currentEntry.tokenIndex);
//                if (this.showRuleStack) {
//                    printRuleState(callStack);
//                }
//            }

            switch (currentEntry.state.getStateType()) {
                case ATNState.RULE_START: // Happens only for the first state in this rule, not subrules.
                    indentation += "  ";
                    break;

                case ATNState.RULE_STOP: {
                    // Record the token index we are at, to report it to the caller.
                    result.add(currentEntry.tokenIndex);
                    continue;
                }

                default:
                    break;
            }

            Transition[] transitions = currentEntry.state.getTransitions();
            for (Transition transition : transitions) {
                switch (transition.getSerializationType()) {
                    case Transition.RULE: {
                        Set<Integer> endStatus = this.processRule(transition.target, currentEntry.tokenIndex, callStack, indentation);
                        for (Integer position : endStatus) {
                            statePipeline.addLast(new PipelineEntry(((RuleTransition) transition).followState, position));
                        }
                        break;
                    }

                    case Transition.PREDICATE: {
                        if (this.checkPredicate((PredicateTransition)transition)) {
                            statePipeline.addLast(new PipelineEntry(transition.target, currentEntry.tokenIndex));
                        }
                        break;
                    }

                    case Transition.WILDCARD: {
                        if (atCaret) {
                            if (!this.translateToRuleIndex(callStack)) {
                                for (Integer token : IntervalSet.of(Token.MIN_USER_TOKEN_TYPE, this.atn.maxTokenType).toList()) {
                                    if (!this.ignoredTokens.contains(token)) {
                                        this.candidates.tokens.put(token, new LinkedList<Integer>());
                                    }
                                }
                            }
                        } else {
                            statePipeline.addLast(new PipelineEntry(transition.target, currentEntry.tokenIndex + 1));
                        }
                        break;
                    }

                    default: {
                        if (transition.isEpsilon()) {
                            if (atCaret) {
                                this.translateToRuleIndex(callStack);
                            }
                            // Jump over simple states with a single outgoing epsilon transition.
                            statePipeline.addLast(new PipelineEntry(transition.target, currentEntry.tokenIndex));
                            continue;
                        }

                        IntervalSet set = transition.label();
                        if (set != null && set.size() > 0) {
                            if (transition.getSerializationType() == Transition.NOT_SET) {
                                set = set.complement(IntervalSet.of(Token.MIN_USER_TOKEN_TYPE, this.atn.maxTokenType));
                            }
                            if (atCaret) {
                                if (!this.translateToRuleIndex(callStack)) {
                                    List<Integer> list = set.toList();
                                    boolean addFollowing = list.size() == 1;
                                    for (Integer symbol: list) {
                                        if (!this.ignoredTokens.contains(symbol)) {
//                                            if (showDebugOutput && logger.isLoggable(Level.FINE)) {
//                                                logger.fine("=====> collected: " + this.vocabulary.getDisplayName(symbol));
//                                            }
                                            if (addFollowing) {
                                                this.candidates.tokens.put(symbol, this.getFollowingTokens(transition));
                                            } else {
                                                this.candidates.tokens.put(symbol, new LinkedList<>());
                                            }
                                        } else {
//                                            logger.fine("====> collected: Ignoring token: " + symbol);
                                        }
                                    }
                                }
                            } else {
                                if (set.contains(currentSymbol)) {
//                                    if (showDebugOutput && logger.isLoggable(Level.FINE)) {
//                                        logger.fine("=====> consumed: " + this.vocabulary.getDisplayName(currentSymbol));
//                                    }
                                    statePipeline.addLast(new PipelineEntry(transition.target, currentEntry.tokenIndex + 1));
                                }
                            }
                        }
                    }
                }
            }
        }

        callStack.removeLast();

        // Cache the result, for later lookup to avoid duplicate walks.
        positionMap.put(tokenIndex, result);

        return result;
    }

    private String[] atnStateTypeMap = new String[] {
            "invalid",
            "basic",
            "rule start",
            "block start",
            "plus block start",
            "star block start",
            "token start",
            "rule stop",
            "block end",
            "star loop back",
            "star loop entry",
            "plus loop back",
            "loop end"
        };

//    private String generateBaseDescription(ATNState state) {
//        String stateValue = (state.stateNumber == ATNState.INVALID_STATE_NUMBER) ? "Invalid" : Integer.toString(state.stateNumber);
//        return "[" + stateValue + " " + this.atnStateTypeMap[state.getStateType()] + "] in " + this.ruleNames[state.ruleIndex];
//    }

//    private void printDescription(String currentIndent, ATNState state, String baseDescription, int tokenIndex) {
//
//        StringBuilder output = new StringBuilder(currentIndent);
//
//        StringBuilder transitionDescription = new StringBuilder();
//        if (this.debugOutputWithTransitions && logger.isLoggable(Level.FINER)) {
//            for (Transition transition: state.getTransitions()) {
//                StringBuilder labels = new StringBuilder();
//                List<Integer> symbols = (transition.label() != null) ? transition.label().toList() : new LinkedList<>();
//                if (symbols.size() > 2) {
//                    // Only print start and end symbols to avoid large lists in debug output.
//                    labels.append(this.vocabulary.getDisplayName(symbols.get(0)) + " .. " + this.vocabulary.getDisplayName(symbols.get(symbols.size() - 1)));
//                } else {
//                    for (Integer symbol: symbols) {
//                        if (labels.length() > 0) {
//                            labels.append(", ");
//                        }
//                        labels.append(this.vocabulary.getDisplayName(symbol));
//                    }
//                }
//                if (labels.length() == 0) {
//                    labels.append("ε");
//                }
//                transitionDescription.
//                        append("\n").
//                        append(currentIndent).
//                        append("\t(").
//                        append(labels).
//                        append(") [").
//                        append(transition.target.stateNumber).
//                        append(" ").
//                        append(this.atnStateTypeMap[transition.target.getStateType()]).
//                        append("] in ").
//                        append(this.ruleNames[transition.target.ruleIndex]);
//            }
//
//            if (tokenIndex >= this.tokens.size() - 1) {
//                output.append("<<").append(this.tokenStartIndex + tokenIndex).append(">> ");
//            } else {
//                output.append("<").append(this.tokenStartIndex + tokenIndex).append("> ");
//            }
////            logger.finer(output + "Current state: " + baseDescription + transitionDescription);
//        }
//    }
//
//    private void printRuleState(LinkedList<Integer> stack) {
////        if (stack.isEmpty()) {
////            logger.fine("<empty stack>");
////            return;
////        }
////
////        if (logger.isLoggable(Level.FINER)) {
////            StringBuilder sb = new StringBuilder();
////            for (Integer rule : stack) {
////                sb.append("  ").append(this.ruleNames[rule]).append("\n");
////            }
////            logger.log(Level.FINER, sb.toString());
////        }
//    }

}