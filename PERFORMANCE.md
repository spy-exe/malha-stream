# Metodologia e estado de desempenho

**Input-to-photon: não medido. Ganho: indisponível. Nenhuma melhoria verificada.** O Sunshine foi identificado na LAN e o [Fire TV AFTSSS passou por instalação, interface e dois testes de integração](docs/benchmarks/DEVICE-AFTSSS.md). Ainda faltam sessões instrumentadas comparativas e instrumento físico. APK, testes de runtime e compilação no CT não medem latência do painel.

## Matriz

A: Moonlight oficial instalado/release oficial, com versão e checksum registrados. B1: Artemis release estável `v20.2.6`; B2: Moonlight V+ `v12.13.0`. C: Malha instrumentado, experimento desligado. D: cada hipótese isolada, inclusive guard MiTV quando aplicável. E: combinação aprovada somente depois dos dados; **não existe combinação aprovada atualmente**.

Por variante e condição: três sessões, ≥120 s de telemetria por sessão, ≥30 eventos físicos válidos por sessão. Executar 1080p60 H.264, 1080p60 HEVC quando suportado, 720p60 H.264 para diagnóstico. Manter bitrate, HDR, refresh, qualidade, host, cena, rede, controle e modo TV equivalentes. Intercalar ordem normal/invertida entre sessões. Coletar pelo menos 100 eventos agregados para estimar p95 na ferramenta, e 1000 para p99; abaixo disso as caudas ficam null. Sessões prolongadas são requisito separado.

`upstream` e `lowest-latency` no runner selecionam o pacing latency que o upstream já oferece. Não representam mecanismos novos. `balanced` seleciona outro modo upstream. O perfil experimental só muda as opções do helper no hardware exato. A configuração efetiva precisa ser conferida no JSON antes de comparar.

## Medição física

1. Use a mesma cena determinística no host. `benchmark/scene.html`, quando disponível, pode fornecer uma mudança visual por botão; não altera Sunshine. Registre resolução, refresh e GPU.
2. Filme referência física do input e região de mudança do painel, preferencialmente ≥240 FPS. LED deve estar eletricamente sincronizado ao acionamento, com método documentado; filmar o dedo sem referência elétrica adiciona incerteza.
3. Mantenha a região do LED e do pixel em posição comparável nas gravações para controlar rolling shutter. Fixe exposição, limiar, ROIs e enquadramento; registre parâmetros da câmera e **FPS de captura**, que pode diferir do FPS de reprodução do arquivo slow motion.
4. Anote `recording_id,session_id,input_frame,photon_frame,camera_fps`. `input-to-photon` exige referência do input. Monitor-host versus TV usa `display-relative`: é diferença entre displays, pode ser negativa e não dá input-to-photon absoluto.
5. Guarde gravações brutas e SHA-256. Revise eventos falhos, saturação, inputs sobrepostos e falsos edges; mantenha razões de exclusão sem apagar o arquivo original. Fotodiodo/DAQ pode reduzir incerteza se os dois canais compartilham o mesmo clock calibrado.
6. A ferramenta usa Δframes × 1000/FPS e registra limite conservador de quantização de dois frames. Em 240 FPS isso equivale a ±8,33 ms conservadores; documente erro adicional de referência, rolling shutter e equipamento.

## Comandos realmente suportados

```bash
python3 -m benchmark plan --device firetv --codec h264 --resolution 1080p --fps 60 --bitrate-kbps 20000 --profile upstream --out benchmark/results/plan.json
python3 -m benchmark probe --serial IP_DA_TV:5555 --out benchmark/results/device.json
python3 -m benchmark prepare --serial IP_DA_TV:5555 --plan benchmark/results/plan.json --out benchmark/results/prepared.json
# Inicie a mesma cena manualmente, grave ≥120 s, encerre o stream normalmente.
python3 -m benchmark collect --serial IP_DA_TV:5555 --out benchmark/results/session-01
python3 -m benchmark import-camera --csv eventos.csv --metadata gravações.json --out baseline.json
python3 -m benchmark compare baseline.json candidato.json --out comparação.json
```

O prepare não faz pairing, não inicia jogo e não interrompe sessão Sunshine. Builds de terceiros usam suas próprias configurações/UI; este entry point ADB existe somente no Malha debug. O comando `collect` lê exports de sessões encerradas, sem forçar parada do app. Coleta automatizada, testes em dispositivo e câmera externa têm origens distintas.

Metadados de câmera devem incluir tipo de medição, referência observada, configuração completa comum às versões e gravações `{id,path,sha256,capture_fps}`. A importação confere cada arquivo bruto. Comparar datasets com configuração diferente ou método diferente é erro. Os percentis internos são intervalos locais de pipeline; não entram no campo de input-to-photon.

Extração óptica opcional (OpenCV, já instalado no CT):

```bash
python3 -m benchmark.camera --video gravacao.mp4 --led-roi 10,10,20,20 --screen-roi 200,200,40,40 --capture-fps 240 --recording-id r1 --session-id s1 --out eventos.csv
```

As ROIs são exemplos de coordenadas, não calibração pronta. O detector gera candidatos e arquivo `.audit.json`, com timeouts/inputs sobrepostos preservados. Eventos inicialmente claros não são inventados como inputs; inputs sobrepostos são invalidados. Repetir a ação apenas depois da resposta visível e pelo menos dois segundos. Revisar gravação, referência física e FPS antes de importar. Teste com vídeo sintético valida somente o algoritmo, sem servir como benchmark.

Para trace informacional durante uma sessão autorizada: `python3 scripts/collect-perfetto.py --serial IP_DA_TV:5555 --seconds 120 --out artifacts/session.perfetto-trace`. FrameTimeline exige suporte da versão/driver. Inspecione perdas de trace e data sources ausentes. Gráficos de comparação real: `python3 scripts/plot-comparison.py comparação.json --out artifacts/comparação.png`; não é gerada figura quando os dados comparativos estão indisponíveis.

Intervalo bootstrap de mediana é exploratório e supõe independência de eventos. Com três sessões, eventos correlacionados não podem ser tratados como evidência robusta de ganho; comparar sessões e considerar incerteza/calibração antes de aprovação. Um p95 maior sempre é sinalizado para revisão, inclusive quando está dentro da incerteza.

## Critério de aceitação

Meta: ≥20% menor mediana física em relação a A, com qualidade/resolução/FPS equivalentes, p95 sem regressão relevante, estabilidade prolongada, áudio e controle íntegros e ausência de artefatos/stutter. Resultado zero, negativo ou abaixo da meta será preservado. A ferramenta nunca aprova automaticamente uma melhora: recordings, telemetria, perdas, frame pacing, condições térmicas e qualidade exigem revisão.

`releaseOutputBuffer` é submissão; callback MediaCodec é informação do Surface, possivelmente atrasada/em lote. FPS contador não é FPS visível. Timestamp do host não é subtraído de timestamp do cliente. Esses limites permanecem obrigatórios em toda análise.
