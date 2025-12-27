package org.dromara.sensitive.utils;

import java.util.HashMap;
import java.util.Map;

public class TrieTree {
    private final TrieNode root = new TrieNode();

    private static class TrieNode {
        boolean isEnd = false;
        Map<Character, TrieNode> children = new HashMap<>();
    }

    // 新增敏感词到Trie树
    public void addWord(String word) {
        if (word == null || word.isEmpty()) return;
        TrieNode node = root;
        for (char c : word.toCharArray()) {
            node.children.putIfAbsent(c, new TrieNode());
            node = node.children.get(c);
        }
        node.isEnd = true;
    }

    // 查找第一个匹配的敏感词
    public String findFirst(String text) {
        if (text == null || text.isEmpty()) return null;
        TrieNode node = root;
        int start = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            // 跳过空格（可选，根据业务调整）
            if (Character.isWhitespace(c)) {
                if (node != root) continue;
                else start = i + 1;
                continue;
            }
            if (!node.children.containsKey(c)) {
                node = root;
                i = start;
                start++;
                continue;
            }
            node = node.children.get(c);
            if (node.isEnd) {
                return text.substring(start, i + 1);
            }
        }
        return null;
    }

    // 替换敏感词为指定字符
    public String replaceSensitive(String text, char replaceChar) {
        String result = text;
        String sensitiveWord;
        while ((sensitiveWord = findFirst(result)) != null) {
            result = result.replace(sensitiveWord, String.valueOf(replaceChar).repeat(sensitiveWord.length()));
        }
        return result;
    }
}
