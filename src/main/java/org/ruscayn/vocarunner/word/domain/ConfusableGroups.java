package org.ruscayn.vocarunner.word.domain;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class ConfusableGroups {

    private final Map<Long, Set<Long>> confusableByWordId;
    private final int groupCount;

    public ConfusableGroups(Collection<? extends Collection<Long>> groups) {
        this.confusableByWordId = toConfusableMap(groups);
        this.groupCount = groups.size();
    }

    private static Map<Long, Set<Long>> toConfusableMap(Collection<? extends Collection<Long>> groups) {
        Map<Long, Set<Long>> map = new HashMap<>();
        for (Collection<Long> group : groups) {
            addConfusableWordIds(map, group);
        }
        return map.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> Set.copyOf(entry.getValue())));
    }

    private static void addConfusableWordIds(Map<Long, Set<Long>> map, Collection<Long> group) {
        for (Long wordId : group) {
            group.stream()
                    .filter(other -> !other.equals(wordId))
                    .forEach(other -> map.computeIfAbsent(wordId, k -> new HashSet<>()).add(other));
        }
    }

    public Set<Long> confusableWordIds(long wordId) {
        return confusableByWordId.getOrDefault(wordId, Set.of());
    }

    public int getGroupCount() {
        return groupCount;
    }

    public int getWordCount() {
        return confusableByWordId.size();
    }

}
