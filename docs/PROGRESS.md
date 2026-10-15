# Progresso por ciclo

1. **Inventário:** PVE 9.2.4, 8 CPUs, ~17 GiB RAM, root quase cheio e thin pool 98,4%. Provisionamento adiado até capacidade segura. Usuário autorizou preservar e remover CTs existentes entre 205 e 218.
2. **Preservação:** fontes instaladas commitadas em repositório privado; arquivos sensíveis fora do Git/sem segredos em plaintext. Backups completos AES-256-GCM com validação de arquivo e hashes remotos. Primeiro backup com download completo; demais por integridade criptográfica do servidor e round-trip local. Um restore real de filesystem foi exercitado em CT temporário desligado. Resultados de boot são NOT_RUN.
3. **Baseline:** fork real publicado com upstream; build original validado em CI antes das mudanças do aplicativo. Ajustes somente de setup SDK foram necessários. Build original também passou no CT.
4. **Diagnóstico:** memória limitada, timestamps locais, origem de dados e nulls para métricas indisponíveis; package próprio. RED → GREEN JVM, lint e APKs aprovados. A versus C físico permanece BLOCKED.
5. **Experimento isolado:** guard MiTV/API34/decoder exato escrito por TDD. Nenhuma alteração em RFI, CSD, frames de referência ou controles. Flag manual desligada por padrão e ausente na release. Compilado/testado; sem ganho medido.
6. **Ferramentas:** runner de plano/configuração/coleta, análise de câmera e diferenças relativas, detector óptico com auditoria, trace e plot real-data-only. RED → GREEN Python; cobertura ≥85%. Nenhuma sessão de hardware foi inventada.

Métricas anteriores/atuais de input-to-photon: **indisponíveis em todos os ciclos**. Referências de build/testes e commits ficam em [RESULTS.md](benchmarks/RESULTS.md). O próximo ciclo físico está preparado, mas depende de TV/ADB e instrumento externo.
