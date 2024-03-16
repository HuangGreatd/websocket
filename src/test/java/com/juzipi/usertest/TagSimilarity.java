package com.juzipi.usertest;

import java.util.*;

public class TagSimilarity {
    // 计算余弦相似度
    public static double cosineSimilarity(List<String> user1Tags, List<String> user2Tags) {
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
            return 0.0; // 处理分母为0的情况
        }

        return dotProduct / (Math.sqrt(normUser1) * Math.sqrt(normUser2));
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
