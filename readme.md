# 大模型智能助手
## 主流程图

```plantuml
graph LR
    A[用户输入问题] --> B[意图确认阶段]
    B --> C[向量数据库检索]
    
    subgraph 数据召回交互
    C --> D[密集检索<br/>向量相似度]
    E[稀疏检索<br/>关键词匹配]
    D --> F[混合检索结果<br/>混合排序]
    E --> F
    end
    
    F --> G[大模型分析意图]
    G --> H{意图确认界面}
    H -->|确认| I[主聊天页面]
    H -->|修改| A
    
    
    subgraph 主聊天阶段
    I --> J[意图语句]
    J --> K[FixHistoryQueryTransformer（基于历史记录的改写器）]
    K --> L{是否有历史对话?}
    L -->|否| M[直接使用原始查询]
    L -->|是| N[历史对话改写]
    M --> P[大模型改写查询]
    N --> O[大模型改写查询]
    
    O --> P[HybridRetriever（混合召回器）]
    P --> Q[稠密检索]
    P --> R[稀疏检索]
    Q --> S[混合排序]
    R --> S
    
    S --> T[StepByStepThinkContentInjector（基于召回数据的逐步分析提示词构造器）]
    T --> U[注入检索内容]
    U --> V[大模型分析]
    V --> W[生成思考链]
    W --> X[FinalAnswerPrompt（构造最终答案提示词）]
    X --> Y[大模型生成最终答案]
    end
    
    subgraph 输出结果
    Y --> Z[AnswerVO]
    Z --> AA[answer: 最终答案]
    Z --> AB[thoughtChain: 思考过程]
    end

    style A fill:#f9f,stroke:#333,stroke-width:2px
    style G fill:#fbb,stroke:#333,stroke-width:2px
    style I fill:#bfb,stroke:#333,stroke-width:2px
    style Y fill:#bff,stroke:#333,stroke-width:2px
    style O fill:#fbb,stroke:#333,stroke-width:2px
    style V fill:#fbb,stroke:#333,stroke-width:2px
    style Y fill:#fbb,stroke:#333,stroke-width:2px
    style H fill:#bfb,stroke:#333,stroke-width:2px



```

