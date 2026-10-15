# Resultados verificáveis

## Engenharia executada

| Verificação | Resultado | Evidência |
|---|---|---|
| Upstream original 12.2, build e lint em CI | PASS | [run 38652453537](https://github.com/spy-exe/malha-stream/actions/runs/38652453537), source `b48494cb` |
| Upstream original no CT Debian/JDK21 | PASS | Log local `ct-baseline-build.log`, 59 tasks, 6m43s; duração de build não é benchmark de streaming |
| Fork debug/release, lint, APK de integração | PASS | [run 38658648113](https://github.com/spy-exe/malha-stream/actions/runs/38658648113), commit `2867d5b7` |
| Fork no CT, mesmas tasks e cobertura | PASS | Log local `ct-malha-build.log`, source `2867d5b7` |
| JVM | 12 testes PASS, cobertura 61/61 linhas (100%) | JaCoCo XML/HTML nos artefatos CI e CT |
| Python puro | 14 testes PASS, 108/111 statements (97,3%) | Coverage XML, source analysis/events/pipeline; CLI/ADB/OpenCV são bindings |
| Integração Android | NOT_RUN | APK de teste compilado; nenhum dispositivo autorizado conectado |
| SSH do laboratório | PASS | Usuário malha, chave, sem sudo; systemd running, sem nesting |
| ADB e mDNS no laboratório | Nenhum dispositivo | Log local `ct-access-and-adb.log` |
| Extração OpenCV | Teste sintético de wrapper | Somente confiabilidade de software; não mede dispositivo nem input-to-photon |
| Recuperação de CT antigo | PASS filesystem | CT 205 restaurado em CT 106 temporário desligado; arquivos conferidos e CT temporário removido. Boot do aplicativo antigo não foi testado. |

Os runs TDD de falha inicial existem no histórico e nos logs locais. Falhas de preparação SDK também foram preservadas: pacote antigo `tools` removido e nome correto da plataforma `android-37.0`. Não foram ocultadas como builds aprovados.

## Comparação de desempenho

| Condição | A | B1/B2 | C | D | E | Ganho físico |
|---|---|---|---|---|---|---|
| 1080p60 H.264 | NOT_RUN | NOT_RUN | NOT_RUN | NOT_RUN | Sem combinação aprovada | indisponível |
| 1080p60 HEVC | NOT_RUN | NOT_RUN | NOT_RUN | NOT_RUN | Sem combinação aprovada | indisponível |
| 720p60 H.264 | NOT_RUN | NOT_RUN | NOT_RUN | NOT_RUN | Sem combinação aprovada | indisponível |

**Medições físicas válidas coletadas: 0.** Não existem CSVs de sessões reais, gráficos comparativos reais, mediana física nem p95/p99 físicos. Arquivos de teste são fixtures sintéticos e planos têm métricas null. Não declarar ganho zero: zero também exigiria medição.

## Experimentos

- **Preparado:** omitir helper low latency somente no MiTV-AYFR0/API34/C2 HEVC exato, por opt-in. Guard testado; resultado físico BLOCKED.
- **Padrão:** pipeline, seleção, RFI e flags upstream. Sem otimização nova automática.
- **Não aprovados:** flags extras Qualcomm/MediaTek, mudança de renderer, descarte de referências, V-Sync desligado e boosts de scheduling. Não há evidência que justifique habilitá-los.

Melhor configuração ou melhor cliente no hardware do usuário: ainda indisponíveis. Próximo ciclo: identificar TV/Fire Stick via ADB autorizado, medir A/B1/B2/C equivalentes e overhead do diagnóstico, coletar câmera/LED e somente então aprovar/reverter D. O Sunshine atual foi preservado.
