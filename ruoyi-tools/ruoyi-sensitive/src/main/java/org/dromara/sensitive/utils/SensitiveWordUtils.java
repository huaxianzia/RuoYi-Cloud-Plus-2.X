package org.dromara.sensitive.utils;

import lombok.extern.slf4j.Slf4j;
import org.dromara.sensitive.config.SensitiveProperties;
import org.dromara.sensitive.domain.SysSensitiveWord;
import org.dromara.sensitive.service.ISysSensitiveWordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * 敏感词工具类（最终稳定版：解决匹配不到/特殊字符/索引异常问题）
 */
@Slf4j
@Component
public class SensitiveWordUtils {
    // 实例变量：Trie树（非静态）
    private TrieTree currentTrie = new TrieTree();
    // 读写锁：保证线程安全
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    // Spring注入依赖
    @Autowired
    private ISysSensitiveWordService sensitiveWordService;
    @Autowired
    private SensitiveProperties sensitiveProperties;


    /**
     * 容器启动后初始化敏感词库
     */
    @EventListener(ContextRefreshedEvent.class)
    public void initSensitiveWordLibrary() {
        log.info("【敏感词库】开始初始化...");
        refresh();
    }


    /**
     * 定时刷新敏感词库（每10分钟）
     */
    @Scheduled(cron = "0 0/10 * * * ?")
    public void refresh() {
        if (!sensitiveProperties.isEnable()) {
            log.warn("【敏感词库】功能未启用，跳过刷新");
            return;
        }

        lock.writeLock().lock();
        try {
            // 1. 查询启用的敏感词
            List<SysSensitiveWord> words = sensitiveWordService.selectEnabledSensitiveWords();
            List<String> wordList = words.stream()
                .map(SysSensitiveWord::getWord)
                .collect(Collectors.toList());
            log.info("【敏感词库】加载敏感词数量：{}，列表：{}", words.size(), wordList);

            // 2. 重建Trie树（极简逻辑，保证添加成功）
            TrieTree newTrie = new TrieTree();
            for (SysSensitiveWord word : words) {
                String cleanWord = word.getWord() == null ? "" : word.getWord().trim();
                if (cleanWord.length() >= 1) { // 允许单字符敏感词（按需调整）
                    newTrie.addWord(cleanWord);
                    log.debug("【敏感词库】添加敏感词：{}", cleanWord);
                } else {
                    log.warn("【敏感词库】跳过无效敏感词：{}", word.getWord());
                }
            }
            currentTrie = newTrie;

            log.info("【敏感词库】刷新完成");
        } catch (Exception e) {
            log.error("【敏感词库】刷新失败", e);
        } finally {
            lock.writeLock().unlock();
        }
    }


    /**
     * 核心方法：检查文本是否含敏感词（返回第一个匹配的词，无则返回null）
     * 极简逻辑，优先保证匹配成功率
     */
    public String check(String text) {
        // 基础校验
        if (!sensitiveProperties.isEnable()) {
            log.debug("【敏感词检查】功能未启用，放行文本：{}", text);
            return null;
        }
        if (text == null || text.trim().isEmpty()) {
            log.debug("【敏感词检查】文本为空，放行");
            return null;
        }

        lock.readLock().lock();
        try {
            // 清理文本空格（避免空格干扰匹配）
            String cleanText = text.replaceAll("\\s+", "");
            // 调用Trie树匹配（极简逻辑，直接返回匹配词）
            String matchWord = currentTrie.findFirst(cleanText);
            log.debug("【敏感词检查】原文本：{} → 清理后：{} → 匹配结果：{}",
                text, cleanText, matchWord == null ? "无" : matchWord);
            return matchWord;
        } catch (Exception e) {
            log.error("【敏感词检查】异常，文本：{}", text, e);
            return null;
        } finally {
            lock.readLock().unlock();
        }
    }


    /**
     * 替换文本中的敏感词
     */
    public String replace(String text) {
        if (!sensitiveProperties.isEnable() || text == null || text.trim().isEmpty()) {
            return text;
        }

        lock.readLock().lock();
        try {
            return currentTrie.replaceSensitive(text, sensitiveProperties.getReplaceChar());
        } catch (Exception e) {
            log.error("【敏感词替换】异常，文本：{}", text, e);
            return text;
        } finally {
            lock.readLock().unlock();
        }
    }


    /**
     * Trie树内部类（极简稳定版，去掉复杂索引逻辑）
     */
    private static class TrieTree {
        // 根节点
        private final TrieNode root = new TrieNode();

        /**
         * Trie节点
         */
        private static class TrieNode {
            boolean isEnd; // 是否为敏感词结尾
            Map<Character, TrieNode> children = new java.util.HashMap<>(16);
        }

        /**
         * 添加敏感词（极简逻辑，支持所有字符，无过滤）
         */
        public void addWord(String word) {
            if (word == null || word.isEmpty()) {
                return;
            }
            TrieNode node = root;
            for (char c : word.toCharArray()) {
                // 不过滤任何字符（包括@、#、数字等）
                node.children.putIfAbsent(c, new TrieNode());
                node = node.children.get(c);
            }
            node.isEnd = true; // 标记词尾
        }

        /**
         * 查找第一个匹配的敏感词（极简逻辑，无复杂截取）
         */
        public String findFirst(String text) {
            if (text == null || text.isEmpty()) {
                return null;
            }

            TrieNode node = root;
            int start = 0; // 敏感词起始索引
            int len = text.length();

            for (int i = 0; i < len; i++) {
                char c = text.charAt(i);
                // 字符不匹配，重置状态
                if (!node.children.containsKey(c)) {
                    node = root;
                    start = i + 1;
                    continue;
                }
                // 字符匹配，深入节点
                node = node.children.get(c);
                // 匹配到词尾，返回完整敏感词
                if (node.isEnd) {
                    String match = text.substring(start, i + 1);
                    log.debug("【Trie树】匹配到敏感词：{}（索引：{}~{}）", match, start, i);
                    return match;
                }
            }
            // 无匹配
            log.debug("【Trie树】文本{}未匹配到敏感词", text);
            return null;
        }

        /**
         * 替换敏感词（极简逻辑）
         */
        public String replaceSensitive(String text, char replaceChar) {
            if (text == null || text.isEmpty()) {
                return text;
            }
            StringBuilder sb = new StringBuilder(text);
            String cleanText = text.replaceAll("\\s+", "");
            TrieNode node = root;
            int start = 0;
            int len = cleanText.length();
            // 原文本和清理文本的索引映射
            int[] originIdxMap = buildOriginIndexMap(text);

            for (int i = 0; i < len; i++) {
                char c = cleanText.charAt(i);
                if (!node.children.containsKey(c)) {
                    node = root;
                    start = i + 1;
                    continue;
                }
                node = node.children.get(c);
                if (node.isEnd) {
                    // 替换原文本中的敏感词
                    int originStart = originIdxMap[start];
                    int originEnd = originIdxMap[i];
                    for (int j = originStart; j <= originEnd; j++) {
                        if (!Character.isWhitespace(sb.charAt(j))) {
                            sb.setCharAt(j, replaceChar);
                        }
                    }
                    // 重置状态
                    node = root;
                    start = i + 1;
                }
            }
            return sb.toString();
        }

        /**
         * 构建原文本索引映射（解决空格偏移）
         */
        private int[] buildOriginIndexMap(String originText) {
            String clean = originText.replaceAll("\\s+", "");
            int[] map = new int[clean.length()];
            int idx = 0;
            for (int i = 0; i < originText.length() && idx < clean.length(); i++) {
                if (!Character.isWhitespace(originText.charAt(i))) {
                    map[idx++] = i;
                }
            }
            return map;
        }
    }
}
