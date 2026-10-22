package br.com.revisaciclo.ui.common

import android.content.res.Resources
import br.com.revisaciclo.R

private val mensagens = mapOf(
    "Backup salvo com sucesso." to R.string.message_00,
    "Não foi possível salvar o backup." to R.string.message_01,
    "Arquivo JSON exportado." to R.string.message_02,
    "Não foi possível exportar o JSON." to R.string.message_03,
    "Não foi possível preparar o arquivo para compartilhar." to R.string.message_04,
    "Não foi possível salvar a avaliação. Tente novamente." to R.string.message_05,
    "Informe ao menos um intervalo." to R.string.message_06,
    "Use apenas números inteiros separados por vírgula." to R.string.message_07,
    "Os intervalos precisam ser maiores que zero." to R.string.message_08,
    "Os intervalos precisam ser estritamente crescentes (ex.: 1, 3, 7)." to R.string.message_09,
    "Cada intervalo pode ter no máximo 3650 dias." to R.string.message_10,
    "Use no máximo 20 estágios de revisão." to R.string.message_11,
    "O arquivo é maior que o limite permitido." to R.string.message_12,
    "Não foi possível ler o arquivo selecionado." to R.string.message_13,
    "Sem permissão para acessar o arquivo selecionado." to R.string.message_14,
    "Não foi possível gravar os dados restaurados. Nada foi alterado." to R.string.message_15,
    "Os dados foram restaurados, mas as preferências serão concluídas quando você reabrir o aplicativo." to R.string.message_16,
    "O arquivo está vazio." to R.string.message_17,
    "O arquivo não é um JSON válido." to R.string.message_18,
    "O arquivo não é um backup do RevisaCiclo." to R.string.message_19,
    "O backup foi criado por uma versão mais nova do aplicativo." to R.string.message_20,
    "O backup parece corrompido (verificação de integridade falhou)." to R.string.message_21,
    "O backup está incompleto." to R.string.message_22,
    "O backup contém valores inválidos." to R.string.message_23,
    "As contagens do cabeçalho não conferem com o conteúdo." to R.string.message_24,
    "Projeto sem nome." to R.string.message_25,
    "Disciplina sem nome." to R.string.message_26,
    "Assunto sem título." to R.string.message_27,
    "Assunto com ciclo inválido." to R.string.message_28,
    "Revisão com intervalo inválido." to R.string.message_29,
    "Há uma disciplina ligada a um projeto inexistente." to R.string.message_30,
    "Há um assunto ligado a uma disciplina inexistente." to R.string.message_31,
    "Há uma revisão ligada a um assunto inexistente." to R.string.message_32,
    "Meta diária fora do intervalo permitido." to R.string.message_33,
    "Intervalos de revisão inválidos." to R.string.message_34,
    "O projeto ativo não existe no backup." to R.string.message_35,
    "Horário de notificação inválido." to R.string.message_36,
)

fun Resources.localizarMensagem(mensagem: String): String {
    mensagens[mensagem]?.let { return getString(it) }
    val prefixo = "O backup não pôde ser validado: "
    if (mensagem.startsWith(prefixo)) {
        return getString(R.string.invalid_backup_details, localizarMensagem(mensagem.removePrefix(prefixo)))
    }
    Regex("Dados restaurados: (\\d+) assuntos, (\\d+) revisões[.]").matchEntire(mensagem)?.let {
        return getString(R.string.restored_summary, it.groupValues[1], it.groupValues[2])
    }
    val tabelas = mapOf("projetos" to R.string.backup_projects, "disciplinas" to R.string.backup_subjects,
        "assuntos" to R.string.backup_topics, "revisões" to R.string.backup_reviews)
    for ((nome, id) in tabelas) {
        when (mensagem) {
            "Há registros demais em $nome." -> return getString(R.string.too_many_records, getString(id))
            "Há um ID inválido em $nome." -> return getString(R.string.invalid_record_id, getString(id))
            "Há IDs duplicados em $nome." -> return getString(R.string.duplicate_record_ids, getString(id))
        }
    }
    return mensagem
}
