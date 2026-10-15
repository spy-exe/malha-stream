# Compatibilidade provisória

| Hardware | Política padrão | Experimento especial | Validação Malha em hardware |
|---|---|---|---|
| Fire TV Stick sem 4K | Seleção/errata upstream | Nenhum habilitado; identificar geração e decoder | NOT_RUN |
| Fire TV AFTSSS / sheldonp / API28 / mt8695 | Seleção/errata upstream | MiTV/Amlogic não se aplica | [Instalação, interface e 2 testes runtime PASS](../benchmarks/DEVICE-AFTSSS.md); desempenho pendente |
| Fire TV Stick 4K / 4K Max | Seleção/errata upstream | Nenhum habilitado; não inferir SoC por nome comercial | NOT_RUN |
| Android TV MediaTek | Flags e fallback upstream | Não copiar lista Artemis indiscriminadamente | NOT_RUN |
| Android TV Amlogic | Flags e fallback upstream | Opt-in apenas MiTV-AYFR0/API34/C2 HEVC exato | NOT_RUN |
| Android TV Qualcomm | Recursos e errata upstream | Fences adicionais não habilitados | NOT_RUN |
| Hardware desconhecido | Fallback upstream | Nenhuma otimização nova automática | NOT_RUN |

O guard possui testes JVM de modelos, API, codec e decoder; isso valida decisão de software, não desempenho do chip. Não usar `IMPROVEMENT VERIFIED` para nenhuma linha desta tabela até medir input-to-photon.
