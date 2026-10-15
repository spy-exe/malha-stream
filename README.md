# Malha Stream

Laboratório de streaming Android TV/Fire OS baseado em Moonlight Android 12.2 e compatível com Sunshine existente.

**Estado:** BUILD READY e [DEVICE VERIFIED no Fire TV AFTSSS](docs/benchmarks/DEVICE-AFTSSS.md), limitado à instalação, interface e dois testes de integração. Sessões instrumentadas comparativas e medição externa ainda estão pendentes. Nenhum ganho físico é anunciado.

- [Pesquisa e escolha de base](RESEARCH.md)
- [Uso passo a passo: instalação, Sunshine, coleta e recuperação](USAGE.md)
- [Arquitetura](ARCHITECTURE.md)
- [Testes e cobertura](TESTING.md)
- [Metodologia de desempenho](PERFORMANCE.md)
- [Compatibilidade provisória](docs/research/COMPATIBILITY.md)
- [Histórico](CHANGELOG.md)
- [Resultados e pendências](docs/benchmarks/RESULTS.md)

## Instalação

Baixe os APKs e confira SHA-256 nos [Releases](https://github.com/spy-exe/malha-stream/releases). O diagnóstico usa `app.malhastream.debug`; a release usa `app.malhastream.unofficial`. Ambos podem coexistir com Moonlight oficial. A release normal mantém políticas upstream, com diagnóstico e experimento desativados.

No diagnóstico, ative **Gravar diagnóstico da sessão** nas configurações e encerre normalmente o stream para exportar. **Exportar última sessão** compartilha JSON/CSV quando existe app de compartilhamento; coleta ADB funciona sem esse app. Overlay e tela de configurações identificam o commit. Os marcadores Android Trace são outra opção, desligada por padrão.

O único experimento novo é HEVC no caso MiTV-AYFR0/API34/decoder C2 exato. É manual, desligado por padrão e não representa otimização aprovada. Hardware desconhecido usa upstream.

## Build reproduzível

```bash
git clone --recurse-submodules https://github.com/spy-exe/malha-stream.git
cd malha-stream
./gradlew --no-daemon --max-workers=2 :telemetry:check :app:assembleNonRootDebug :app:lintNonRootDebug
python3 -m unittest discover -s tests -v
```

Requer JDK 17+ compatível, Android SDK `platforms;android-37.0`, Build Tools 36.0.0 e NDK 29.0.14206865. O wrapper fixa Gradle 9.7.1 e AGP 9.4.0. Scripts de instalação nativa existem em `scripts/`, com guard para executar somente no CT do projeto. O laboratório usa JDK 21 no Debian; CI usa JDK 17. Logs registram a diferença, sem prometer APK byte a byte idêntico entre ambientes.

CI executa checkout com submodules, build debug/release, lint, testes JVM, cobertura, testes Python, montagem do APK de integração e checksums. Publica artefatos de desenvolvimento. Testes Android reais permanecem NOT_RUN até haver dispositivo autorizado; APK de teste compilado não significa teste executado.

## Atualização do upstream

```bash
git remote add upstream https://github.com/moonlight-stream/moonlight-android.git # se ausente
git fetch upstream --tags
git switch -c maintenance/upstream-sync
# Revise diff e submodule; integre a revisão escolhida e execute TESTING.md.
```

Mantenha `main` com defaults seguros. Use branches curtas para hipóteses, preserve o baseline por commit e não atualize protocolo, decoder e apresentação no mesmo experimento. Mudança no submodule exige testes próprios de conexão/recovery; não executar merge sem resolver/revisar conflitos.

## Licença e créditos

GPL-3.0, conforme [LICENSE.txt](LICENSE.txt), com licenças das dependências preservadas. Fork de [Moonlight Android](https://github.com/moonlight-stream/moonlight-android), de Cameron Gutman, Diego Waxemberg, Aaron Neyer, Andrew Hennessy e demais contribuidores. [README original](docs/UPSTREAM_README.md) preservado. Não distribuir segredos nem chaves de assinatura no Git. Correspondência entre APK, commit e checksum acompanha cada release.
