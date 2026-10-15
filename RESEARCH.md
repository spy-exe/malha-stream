# Malha Stream — pesquisa técnica

Consulta: **2026-10-14**. Resultados de terceiros e inspeção de código não são benchmarks físicos deste projeto.

## Fontes e versões fixadas

| Fonte | Commit/versão inspecionada | Observação |
|---|---|---|
| [Moonlight Android](https://github.com/moonlight-stream/moonlight-android/commit/b48494cb96bff23d8886c4775cc4f39a1075495d) | `b48494cb96bff23d8886c4775cc4f39a1075495d`, 2026-09-12, 12.2 | Base escolhida; build e lint executados |
| [Decoder errata](https://github.com/moonlight-stream/moonlight-android/blob/b48494cb96bff23d8886c4775cc4f39a1075495d/decoder-errata.txt) | Mesmo commit | Correções variam entre drivers; reduzir buffers indiscriminadamente pode aumentar latência |
| [MediaCodecHelper](https://github.com/moonlight-stream/moonlight-android/blob/b48494cb96bff23d8886c4775cc4f39a1075495d/app/src/main/java/com/limelight/binding/video/MediaCodecHelper.java) | Mesmo commit | Seleção, recursos declarados, flags e fallbacks já existem |
| [Renderer](https://github.com/moonlight-stream/moonlight-android/blob/b48494cb96bff23d8886c4775cc4f39a1075495d/app/src/main/java/com/limelight/binding/video/MediaCodecDecoderRenderer.java) | Mesmo commit | Surface direto, fila balanceada limitada a dois buffers e recuperação de codec |
| [Artemis](https://github.com/ClassicOldSong/moonlight-android/tree/c5cf27f4dc822db0e863c4691e7a70c74bea977a) | `c5cf27f4dc822db0e863c4691e7a70c74bea977a`, 2026-09-09 | HEAD examinado; release estável publicada `v20.2.6`, 2025-08-08, deve ser diferenciada de HEAD |
| [Moonlight V+](https://github.com/qiin2333/moonlight-vplus/tree/edc7d69efc34cea7218933d79638f83165a280e1) | `edc7d69efc34cea7218933d79638f83165a280e1`, 2026-10-14 | Release estável comparativa `v12.13.0`, 2026-10-11; possui política HEVC específica |
| [Workaround MiTV](https://github.com/Viktsolovevwork278/moonlight-android-hevc-fix/commit/eac0c98326daf6edc6cc7958e9bd1f004291c7fb) | `eac0c98326daf6edc6cc7958e9bd1f004291c7fb`, 2026-08-07 | Mudanças posteriores à correção também existem; não copiar HEAD como um patch único |
| [Moonlight Turbo](https://github.com/alonsojr1980/moonlight-android-turbo/commit/62bff92e38003ad06f532a7cf4d93ec358a8490f) | `62bff92e38003ad06f532a7cf4d93ec358a8490f`, 2025-04-14 | Autor direciona usuários ao Artemis; flags Qualcomm/Exynos não comprovam ganho em TV |
| [Moonlight Common C](https://github.com/moonlight-stream/moonlight-common-c/commit/aeba988e0a302145eff2e70fa3e11cd98ec01f5d) | HEAD consultado `aeba988e0a302145eff2e70fa3e11cd98ec01f5d`, 2026-10-14 | Fork mantém submodule upstream `874ac9548f1bd6f095ef2b435c42cdde460e7821`; não atualiza protocolo junto com hipótese de decoder |
| [Sunshine](https://github.com/LizardByte/Sunshine/commit/a178f465e42f8e77102002e9156b8becd7b13628) | HEAD `a178f465e42f8e77102002e9156b8becd7b13628`, 2026-10-14 | O host instalado permanece intacto; versão do host real ainda deve ser registrada |
| [Aurora](https://github.com/Koloses/aurora-android/tree/4c8d1aff08e85c758543a42cf8f07572f062d5d3) | `4c8d1aff08e85c758543a42cf8f07572f062d5d3`, 2026-07-04 | PyroWave/Vulkan exige Solarflare para esse codec; fora do escopo de preservar Sunshine e usar decoders dos sticks |

## Issue 1584 e limites da evidência

A [issue #1584](https://github.com/moonlight-stream/moonlight-android/issues/1584) foi aberta em 2026-07-27 e continuava aberta na consulta. O relato descreve MiTV-AYFR0, Android TV 14, decoder `c2.amlogic.hevc.decoder`, HEVC em slideshow apesar do contador de FPS normal. Desabilitar as opções MediaCodec foi relatado como correção; desligar RFI não resolveu. Há relato adicional de Xiaomi TV Box S2 e dúvida sobre estabilidade prolongada, sem amostra física controlada publicada na discussão.

A referência de setembro é o [commit `6158b74` do Moonlight V+](https://github.com/qiin2333/moonlight-vplus/commit/6158b74102e768b64ce9a0780406bcf29314c182), **não uma correção encontrada no HEAD Artemis inspecionado**. V+ usa AUTO/OFF/ON e, em AUTO, reconhece prefixos OMX/C2 Amlogic para HEVC. Seu código separa a tentativa de prioridade realtime de outros ajustes. A generalização para toda a família deve ser avaliada contra as exceções Fire OS existentes.

Confirmado aqui: conteúdo do código, versões, existência dos relatos e builds. Não confirmado aqui: causa do atraso dos dispositivos do usuário, frequência real de apresentação, efeito térmico, input-to-photon ou vantagem de qualquer fork. Os 6,5 ms informados ainda não foram identificados; o campo de host processing do protocolo é uma duração reportada pelo host, diferente de tempo isolado de encode e de latência física total.

## Mecanismos já existentes e decisões

1. **Low latency:** [AOSP](https://source.android.com/docs/core/media/low-latency-media) documenta API desde Android 11 e dependência de implementação do driver. Suporte declarado não garante comportamento correto. Upstream já tenta `low-latency`, `vdec-lowlatency` e extensões conhecidas com fallback. Não adicionar todas as flags simultaneamente.
2. **Seleção de decoder:** upstream examina recursos declarados e errata; Pixel e Exynos têm variantes com diferenças de suporte. Malha registra inventário e seleção efetiva, sem criar um ranking baseado somente em nomes.
3. **Buffers e referências:** errata inclui num_ref_frames, restrições SPS e casos MediaTek que pioram com buffer mínimo. Manter CSD/SPS/PPS, RFI e recuperação upstream. Nenhum frame comprimido de referência é descartado pelo experimento.
4. **Apresentação:** upstream já usa decoder → Surface e descarta outputs antigos já decodificados em determinados modos. O modo balanceado mantém até dois outputs. Trocar SurfaceView por TextureView/GL adicionaria trabalho sem evidência que justifique isso.
5. **V-Sync e display:** `Game.java` já escolhe modos e chama `setFrameRate`. A [documentação Android](https://developer.android.com/media/optimize/performance/frame-rate) descreve múltiplos de FPS e diferenças entre troca de modo e hint. Não desativar V-Sync nem refazer seleção de refresh sem trace.
6. **Callback de render:** [OnFrameRenderedListener](https://developer.android.com/reference/android/media/MediaCodec.OnFrameRenderedListener) pode entregar callbacks atrasados/em lote e, antes do Android 14, omiti-los. Seu timestamp informa render no Surface; nem callback nem retorno de release confirmam pixels no painel.
7. **SurfaceFlinger:** composição, latch e scanout precisam de trace no dispositivo. A camada de diagnóstico registra apenas os pontos que pode observar; medição externa cobre o restante.
8. **Zero-copy:** o output já vai ao Surface. Ainda há cópia de bitstream no caminho de input; removê-la exige medir custo e preservar vida útil dos buffers JNI/MediaCodec. Não foi alterada.
9. **Codec/resolução:** H.264/HEVC podem inverter o melhor resultado conforme driver. 720p60 serve para diagnóstico de capacidade; não pode vencer baseline 1080p60 em comparação de ganho.
10. **Áudio e controle:** os caminhos upstream foram preservados. Eventos Android e envio de estado do controle são componentes locais; Bluetooth, USB, host/game loop e TV devem permanecer separados na metodologia.
11. **Scheduling/ADPF:** renderer e receive threads já têm políticas upstream. [ADPF](https://developer.android.com/games/optimize/adpf) e [Thermal API](https://developer.android.com/games/optimize/adpf/thermal) dependem de plataforma/hardware. Malha amostra status térmico disponível; não inventa temperatura do SoC nem aplica boost sem profiling.
12. **Fire OS:** errata registra Fire TV 3 que necessita `vdec-lowlatency` para HEVC produzir saída. Uma proibição ampla de flags Amlogic poderia quebrar esse caso. Fire Stick sem 4K precisa de identificação de geração/modelo; o nome comercial não basta.

## Upstream versus Artemis e V+

| Aspecto | Upstream | Artemis HEAD examinado | V+ examinado |
|---|---|---|---|
| Base para manutenção | Menor delta, políticas consolidadas | Delta grande de UI, virtual display, apresentação e flags | Delta grande, funcionalidades adicionais e política HEVC |
| Qualcomm | Extensões conhecidas e seleção por recursos | Adiciona fences e tratamento OMX/C2 de picture order | Examinar perfil selecionado em execução |
| MediaTek | `vdec-lowlatency` e errata | Adiciona boosts/DVFS, parâmetros de fila/timeout e opções de vendor | Não há evidência física deste laboratório |
| Apresentação | Modos latency/balanced/cap/smoothness | Drenagem, thresholds/EWMA e modos adicionais | Alterações adicionais exigem isolamento |
| MiTV Android 14 | Sem guard específico encontrado nesta base | Não encontrado no HEAD examinado | Política explícita ligada à issue |
| Input-to-photon no alvo | Não medido aqui | Não medido aqui | Não medido aqui |

Artemis possui caminhos com várias opções opcionais e exceções amplamente toleradas. A aceitação de uma chave pelo MediaFormat não prova aplicação pelo driver. No ramo MediaTek inspecionado, `setNewOption=true` fora da condição de tentativa também merece revisão se configure falhar repetidamente: trata-se de risco estático, não crash reproduzido neste laboratório.

**Escolha:** upstream 12.2 como base Malha. É a menor mudança compatível com Sunshine existente e permite medir cada hipótese. Artemis e V+ são candidatos comparativos, sem declarar vencedor antes de hardware. A última release estável de cada fork e seu HEAD são identificadores distintos.

## Hipóteses priorizadas

| Prioridade | Hipótese | Potencial / custo | Estado |
|---|---|---|---|
| 1 | Processamento da TV/refresh/composição adiciona atraso fora do decoder | Alto / câmera e controle do modo TV | BLOCKED: acesso físico |
| 2 | Driver HEVC MiTV Android 14 trava com opções low latency | Alto para o caso reportado / patch pequeno | Experimento opt-in, model/API/decoder exatos; não validado fisicamente |
| 3 | Overhead da instrumentação C versus A | Necessário para confiar em diagnósticos / sessões intercaladas | BLOCKED: dispositivo |
| 4 | H.264 versus HEVC no SoC real e pacing já existente | Potencial alto / matriz pequena | Runner e coleta preparados |
| 5 | Jitter/recovery e permanência da fila sob carga térmica | Dependente de trace / custo médio | Telemetria preparada; sem traces reais |
| 6 | Copias JNI, novas flags, scheduling ou novo renderer | Incerto / risco e custo maiores | Não habilitados; dependem das medições anteriores |

Nenhuma hipótese foi aprovada como melhoria de desempenho. O experimento implementado está desativado por padrão, inclusive em hardware correspondente, e indisponível na release normal.
# Atualização de hardware

Após a pesquisa inicial, houve acesso ADB autorizado ao Amazon AFTSSS/sheldonp, Android 9/API28, plataforma mt8695. [Verificação e evidências](docs/benchmarks/DEVICE-AFTSSS.md). Isso identifica o alvo MediaTek e exclui o guard MiTV/Amlogic/API34 deste aparelho; desempenho e input-to-photon continuam pendentes.
