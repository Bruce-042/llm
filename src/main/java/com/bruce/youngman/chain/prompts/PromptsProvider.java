package com.bruce.youngman.chain.prompts;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.content.Content;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Liangyonghui
 * @since 2025/5/10 14:59
 */
public class PromptsProvider {
    public static Prompt confirmIntentPrompt(List<Content> contents, ChatMessage currentMessage, List<ChatMessage> history) {
        StringBuilder promptBuilder = new StringBuilder();

        // 添加历史会话
        if (history != null && !history.isEmpty()) {
            promptBuilder.append("历史对话：\n");
            for (ChatMessage message : history) {
                promptBuilder.append(message.type() == ChatMessageType.USER ? "用户: " : "助手: ")
                        .append(message.type() == ChatMessageType.USER ? ((UserMessage)message).contents() : ((AiMessage)message).text())
                        .append("\n");
            }
            promptBuilder.append("\n");
        }

        // 添加检索到的内容
        promptBuilder.append("你是一个专业的手机质检工程师，针对用户问题进行专业的意图判断助手。\n");
        promptBuilder.append("参考内容：\n");
        for (Content content : contents) {
            promptBuilder.append(content.textSegment().text()).append("\n");
        }
        promptBuilder.append("\n");

        // 添加当前消息
        promptBuilder.append("当前用户消息：").append(((UserMessage)currentMessage).contents()).append("\n");
        promptBuilder.append("  请遵循以下**推理链**（Chain of Thought）逐步分析并作答：\n" +
                "                                \n" +
                "                                1. **提取核心问题**：用户想了解什么？\n" +
                "                                2. **是否存在模糊表达**：\n" +
                "                                   - 是否包含不明确指代（如“这个”、“这种”、“这样”）？\n" +
                "                                   - 是否缺乏具体主语、对象、现象？（如“要算划痕吗”）\n" +
                "                                   - 是否为反问句、否定表达（如“不用判断吧？”）？\n" +
                "                                3. **知识比对**：检索到的知识片段是否能明确用户的意图？\n" +
                "                                4. **综合判断**：\n" +
                "                                   - 若用户的疑问，能够确定咨询的具体问题（描述了具体现象的），请判定为**明确**；\n" +
                "                                   - 若用户提问中，有明显的模糊代指，并且没有明确代指信息，无法确定具体问题（无法判断出工程师所遇现象的），请判定为**不明确**\n" +
                "                                   - 若用户的疑问，无法能够确定提问的意图，判定为**不明确**；\n" +
                "                                \n" +
                "                                【输出格式要求】：\n" +
                "                                  请以严格的JSON格式返回数据，不要包含任何解释或额外文本。" +
                "                                        返回一个包含以下字段的JSON对象:" + "\n" +
                "                                          intentResult: 明确 或 不明确" +
                "                                          intent: 一句话总结用户的意图，询问用户是否想问这个，比如：你是不是想咨询xxxxxx" +
                "                                          thoughtChain: 你的思考分析的全过程思维链路" +
                "                                \n" +
                "                                \n" +
                "                                【重要注意事项】：\n" +
                "                                - 必须基于检索到的知识和用户问题推理，**禁止主观想象或无依据推断**。\n" +
                "                                - 不允许你基于常识、概率、经验去“猜测”用户意思；\n" +
                "                                - 保持输出格式简洁、清晰，不输出额外说明或废话。\n" +
                "                                - 在输出前，务必在内部推理中逐步完成以上步骤，以保证判断严谨、准确。\n" +
                "                                - 如信息缺失，必须要求用户补充，不得代为填空；");

        return Prompt.from(promptBuilder.toString());

    }

    public static Prompt confirmIntentPrompt(List<Content> contents, ChatMessage question) {
        // 构建结构化知识库表示
        StringBuilder knowledgeBuilder = new StringBuilder();
        for (Content content : contents) {
            String title = content.textSegment().metadata().getString("title");
            String text = content.textSegment().text();
            knowledgeBuilder.append(String.format("【%s】\n%s\n\n", title, text));
        }

        // 采用决策树式提示词结构
        String promptTemplate = """
                    你是一个专业的手机质检工程师，针对用户问题进行专业的意图判断助手。
                                请结合以下信息进行思考：
                                - 【检索到的知识库片段】
                                {{contents}}
                                - 【用户的提问】
                                {{question}}
                                
                                请遵循以下**推理链**（Chain of Thought）逐步分析并作答：
                                
                                1. **提取核心问题**：用户想了解什么？
                                2. **是否存在模糊表达**：
                                   - 是否包含不明确指代（如“这个”、“这种”、“这样”）？
                                   - 是否缺乏具体主语、对象、现象？（如“要算划痕吗”）
                                   - 是否为反问句、否定表达（如“不用判断吧？”）？
                                3. **知识比对**：检索到的知识片段是否能明确用户的意图？
                                4. **综合判断**：
                                   - 若用户的疑问，能够确定咨询的具体问题（描述了具体现象的），请判定为**明确**；
                                   - 若用户提问中，有明显的模糊代指，并且没有明确代指信息，无法确定具体问题（无法判断出工程师所遇现象的），请判定为**不明确**
                                   - 若用户的疑问，无法能够确定提问的意图，判定为**不明确**；
                                
                                【输出格式要求】：
                                - 如果判断为**明确**，请输出：明确：<一句话总结用户意图>
                                - 如果判断为**不明确**，请输出：不明确：<基于知识库，提出引导用户明确意图问题>，<若知识库无任何对应知识，请让咨询人工>
                                
                                
                                【重要注意事项】：
                                - 必须基于检索到的知识和用户问题推理，**禁止主观想象或无依据推断**。
                                - 不允许你基于常识、概率、经验去“猜测”用户意思；
                                - 保持输出格式简洁、清晰，不输出额外说明或废话。
                                - 在输出前，务必在内部推理中逐步完成以上步骤，以保证判断严谨、准确。
                                - 如信息缺失，必须要求用户补充，不得代为填空；
                """;

        PromptTemplate template = PromptTemplate.from(promptTemplate);
        return template.apply(Map.of("contents", knowledgeBuilder, "question", question));
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
                4. 如果知识不足以得出结论，请引导工程师后续补充详细情况。
                5. 仔细检查知识的因果关系，不得出现前后矛盾。
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
