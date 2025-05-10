package com.bruce.youngman.chain.prompts;

import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.content.Content;

import java.util.List;
import java.util.Map;

/**
 * @author Liangyonghui
 * @since 2025/5/10 14:59
 */
public class PromptsProvider {


    public static Prompt confirmIndentPrompt(List<Content> contents) {
        String promptTemplate = """
                你是一个意图判断助手。请参考历史对话
                以及检索到的知识库片段：
                {{contents}}
                结合'检索到的知识库片段'、'用户的历史记录'和'当前提问'，
                判断用户问题是否可以在知识库中有对应的知识。
                ——如果**可以**，请输出：
                ```
                明确：
                <一句话总结用户意图>
                ```
                ——如果**不能**，请输出：
                ```
                不明确：
                你是否想咨询：<基于知识库的标题，提出引导工程师明确意图问题>
                ```
                <若知识库无任何对应知识，请让工程师咨询总部培训师>
                ```
                """;

        PromptTemplate template = PromptTemplate.from(promptTemplate);
        return template.apply(Map.of("contents", contents));
    }


}
