# ADR-0001: Multi-Provider LLM Architecture

## Status

Accepted

## Date

2024-12-06

## Context

The HDIM healthcare platform requires AI agent capabilities for clinical decision support, care gap optimization, and report generation. Healthcare environments have strict requirements around:

1. **High Availability** - Clinical workflows cannot tolerate extended AI service outages
2. **HIPAA Compliance** - All LLM providers must support Business Associate Agreements (BAA)
3. **Regional Data Residency** - GDPR and state-specific regulations require data to remain in certain regions
4. **Cost Optimization** - Different use cases have varying latency and cost tolerances
5. **Model Flexibility** - Ability to leverage best-in-class models for specific tasks

Single-provider dependency creates unacceptable risk for healthcare operations where AI agents support clinical decision-making.

## Decision

We will implement a **multi-provider LLM abstraction layer** supporting three providers with health-based routing and automatic failover:

### Primary Provider: Anthropic Claude
- Used for complex clinical reasoning and multi-step agent tasks
- Models: claude-3-opus, claude-3-sonnet, claude-3-haiku
- BAA available for HIPAA compliance

### Secondary Provider: Azure OpenAI
- Used as primary fallback and for EU data residency requirements
- Models: gpt-4-turbo, gpt-4o, gpt-35-turbo
- Azure regions: US East, US West, EU West
- BAA and GDPR compliance through Azure agreements

### Tertiary Provider: AWS Bedrock
- Used for regional availability and cost optimization
- Models: Claude (via Bedrock), Titan, Llama 2
- Integrated with existing AWS infrastructure

### Architecture

```
┌─────────────────────────────────────────────────────┐
│                  LLMProviderFactory                  │
│  ┌─────────────────────────────────────────────────┐│
│  │           Health-Based Router                   ││
│  │  - Circuit breaker state                        ││
│  │  - Latency percentiles                          ││
│  │  - Error rate tracking                          ││
│  │  - Regional affinity                            ││
│  └─────────────────────────────────────────────────┘│
│           │              │              │           │
│     ┌─────▼─────┐  ┌─────▼─────┐  ┌─────▼─────┐    │
│     │  Claude   │  │  Azure    │  │  Bedrock  │    │
│     │ Provider  │  │  OpenAI   │  │  Provider │    │
│     └───────────┘  └───────────┘  └───────────┘    │
└─────────────────────────────────────────────────────┘
```

### Provider Selection Logic

```java
public LLMProvider selectProvider(AgentContext context) {
    // 1. Check regional requirements
    if (context.requiresEUResidency()) {
        return azureOpenAI.getEUEndpoint();
    }

    // 2. Use health-based selection
    List<LLMProvider> healthy = providers.stream()
        .filter(p -> circuitBreaker.isOpen(p))
        .sorted(Comparator.comparing(p -> p.getLatencyP95()))
        .toList();

    // 3. Apply model capability matching
    return healthy.stream()
        .filter(p -> p.supportsModel(context.getRequiredModel()))
        .findFirst()
        .orElseThrow(() -> new NoHealthyProviderException());
}
```

### Fallback Chain

1. **Primary failure** → Route to Azure OpenAI (same model family if available)
2. **Secondary failure** → Route to Bedrock Claude
3. **All providers unhealthy** → Return cached response if available, else graceful degradation

### Configuration

```yaml
hdim:
  ai:
    providers:
      anthropic:
        enabled: true
        priority: 1
        api-key: ${ANTHROPIC_API_KEY}
        baa-signed: true
        regions: [us]
      azure-openai:
        enabled: true
        priority: 2
        endpoint: ${AZURE_OPENAI_ENDPOINT}
        api-key: ${AZURE_OPENAI_KEY}
        baa-signed: true
        gdpr-compliant: true
        regions: [us-east, us-west, eu-west]
      bedrock:
        enabled: true
        priority: 3
        region: ${AWS_REGION}
        baa-signed: true
        regions: [us-east-1, us-west-2]

    fallback:
      max-retries: 3
      retry-delay-ms: 1000
      circuit-breaker:
        failure-threshold: 5
        reset-timeout-ms: 30000
```

## Consequences

### Positive

- **99.9% availability target achievable** through provider redundancy
- **HIPAA/GDPR compliance** maintained across all providers
- **Cost optimization** possible by routing to cheaper providers for non-critical tasks
- **Model flexibility** - can use best model for each use case
- **Vendor independence** - no single provider lock-in
- **Regional compliance** - can route EU traffic to EU endpoints

### Negative

- **Increased complexity** in provider management and monitoring
- **Prompt engineering variance** - different providers may require prompt adjustments
- **Response consistency** - output format may vary between providers
- **Higher operational overhead** - must maintain multiple provider relationships
- **Testing complexity** - must test all provider paths

### Neutral

- Need to maintain BAA agreements with multiple vendors
- Model capabilities will evolve differently across providers
- Cost structures vary and require ongoing optimization

## Implementation Notes

1. **LLMProvider interface** abstracts provider-specific details
2. **LLMProviderFactory** handles selection and failover
3. **CircuitBreaker** (Resilience4j) protects against cascading failures
4. **ProviderHealthMonitor** tracks latency, errors, and availability
5. **PHI filtering** applied before sending to any provider

## References

- [Anthropic Claude API Documentation](https://docs.anthropic.com/)
- [Azure OpenAI Service](https://learn.microsoft.com/en-us/azure/ai-services/openai/)
- [AWS Bedrock Documentation](https://docs.aws.amazon.com/bedrock/)
- [HIPAA BAA Requirements](https://www.hhs.gov/hipaa/for-professionals/covered-entities/sample-business-associate-agreement-provisions/index.html)
- HDIM Plan: `/home/mahoosuc-solutions/.claude/plans/agile-launching-church.md`
