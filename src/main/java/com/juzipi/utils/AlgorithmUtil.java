package com.juzipi.utils;

import java.util.*;


public class AlgorithmUtil {

    /**
     * 最短编辑距离算法
     */
    public static int minDistance(List<String> tagList1, List<String> tagList2) {
        int n = tagList1.size();
        int m = tagList2.size();

        if (n * m == 0) {
            return n + m;
        }
        int[][] d = new int[n + 1][m + 1];
        for (int i = 0; i < n + 1; i++) {
            d[i][0] = i;
        }

        for (int j = 0; j < m + 1; j++) {
            d[0][j] = j;
        }

        for (int i = 1; i < n + 1; i++) {
            for (int j = 1; j < m + 1; j++) {
                int left = d[i - 1][j] + 1;
                int down = d[i][j - 1] + 1;
                int leftDown = d[i - 1][j - 1];
                if (!Objects.equals(tagList1.get(i - 1), tagList2.get(j - 1))) {
                    leftDown += 1;
                }
                d[i][j] = Math.min(left, Math.min(down, leftDown));
            }
        }
        return d[n][m];
    }

    // 计算余弦相似度
    public static long cosineSimilarity(List<String> user1Tags, List<String> user2Tags) {
        // 合并标签，构建标签集合
        Set<String> unionTags = new HashSet<>(user1Tags);
        unionTags.addAll(user2Tags);

        // 构建两个用户的标签向量
        int[] vectorUser1 = buildTagVector(user1Tags, unionTags);
        int[] vectorUser2 = buildTagVector(user2Tags, unionTags);

        // 计算余弦相似度
        double dotProduct = 0.0;
        double normUser1 = 0.0;
        double normUser2 = 0.0;

        for (int i = 0; i < vectorUser1.length; i++) {
            dotProduct += vectorUser1[i] * vectorUser2[i];
            normUser1 += Math.pow(vectorUser1[i], 2);
            normUser2 += Math.pow(vectorUser2[i], 2);
        }

        if (normUser1 == 0 || normUser2 == 0) {
            return 0l; // 处理分母为0的情况
        }

        return (long) ((long) dotProduct / (Math.sqrt(normUser1) * Math.sqrt(normUser2)));
    }

    // 构建标签向量
    private static int[] buildTagVector(List<String> tags, Set<String> unionTags) {
        int[] vector = new int[unionTags.size()];
        List<String> unionList = new ArrayList<>(unionTags);

        for (String tag : tags) {
            int index = unionList.indexOf(tag);
            vector[index] = 1;
        }
        return vector;
    }

    public static void main(String[] args) {
        List<String> user1Tags = Arrays.asList("java", "javaSrcipt", "c++");
        List<String> user2Tags = Arrays.asList("java", "php", "python");

        // 计算余弦相似度
        double similarity = cosineSimilarity(user1Tags, user2Tags);
        System.out.println("余弦相似度: " + similarity);
    }
}
