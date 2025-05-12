package com.bruce.youngman.chain.prompts;

import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.content.Content;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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


    public static Prompt stepByStepAnalysisPrompt(List<Content> contents, String question) {
        Map<String, String> contentsMap = new HashMap<>();
        for (Content content : contents) {
            String text = content.textSegment().text();
        }
        String promptTemplate = """
                # 角色设定（分析阶段）
                你是一名资深的手机质检培训专家助手，精通所提供文档中的所有质检标准与流程。现在你负责针对工程师提出的问题，进行分析

                # 分析要求
                1. 绝不凭空添加任何未在文档中出现的信息。
                2. 分步思考至少包含 4–6 个步骤，每步都要：
                - 简明扼要地描述该步骤在做什么（如"判定鼓包程度"）。
                - 标注依据来源（例如"依据文档第2段""参照标准X-3条款"）。
                3. 步骤涵盖：  
                1) 前提假设；  
                2) 核心判定标准；  
                3) 对比分析过程；  
                4) 最终判断逻辑。 
                4) 如果可能导致衍生的知识，需要一并纳入到我靠中
                5. 如果分析阶段识别出多种情况，请全部在此列出，不要遗漏。

                # 输出格式
                思考过程：  
                Step 1（依据：…）：…  
                Step 2（依据：…）：…  
                …  
                Step N（依据：…）：…

                背景资料：
                {{contents}}

                请先对以下问题进行详细的分步思考，并列出判断依据：
                问题：{{question}}""";
        PromptTemplate template = PromptTemplate.from(promptTemplate);
        return template.apply(Map.of("contents", contents, "question", question));
    }


    public static Prompt finalAnswerPrompt() {
        String promptTemplate = """
                # 角色设定（结论阶段）
                你是一名资深的手机质检培训专家助手，严格依照前面"分步思考"输出内容给出最终结论。

                # 结论要求
                1. 如果分析阶段识别出多种情况，请全部在此列出，不要遗漏。
                2. 以列表形式输出每个"判定选项 + 依据步骤"。
                3. 不要重复分析内容，也不要输出任何额外信息。
                4. 如果知识不足以得出结论，请引导工程师后续补充详细情况；
                """;
        PromptTemplate template = PromptTemplate.from(promptTemplate);
        return template.apply(new HashMap<>());
    }


    public static Prompt finalAnswerPrompt2(String analysis, String question) {
        String promptTemplate = """
                以下是分步思考：
                {{analysis}}

                请基于以上思考给出最准确的结论和推荐选项：
                问题：{{question}}""";
        PromptTemplate template = PromptTemplate.from(promptTemplate);
        return template.apply(Map.of("analysis", analysis, "question", question));
    }


}
