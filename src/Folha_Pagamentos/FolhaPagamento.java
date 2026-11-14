package Folha_Pagamentos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Scanner;

public class FolhaPagamento {

	// Lista as folhas pendentes e deixa confirmar ou apagar
	public static void folhasAConfirmar(Scanner sc, Connection conn, int empresaId) throws SQLException {
		PreparedStatement stmt = conn.prepareStatement(
				"SELECT ff.id, fp.mes, fp.ano, fu.nome, ff.salario_liquido FROM folha_funcionarios ff "
						+ "JOIN folhas_pagamento fp ON fp.id = ff.folha_id "
						+ "JOIN funcionarios fu ON fu.id = ff.funcionario_id "
						+ "WHERE fu.empresa_id = ? AND ff.status = 'PENDENTE' ORDER BY fp.ano, fp.mes, fu.nome");
		stmt.setInt(1, empresaId);
		ResultSet rs = stmt.executeQuery();

		NumberFormat moeda = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));

		// Mostra as folhas pendentes
		System.out.println();
		System.out.println("=========== FOLHAS A CONFIRMAR ===========");
		boolean achou = false;
		while (rs.next()) {
			achou = true;
			System.out.println(rs.getInt("id") + " - " + String.format("%02d/%d", rs.getInt("mes"), rs.getInt("ano"))
					+ " - " + rs.getString("nome") + " | Líquido: " + moeda.format(rs.getDouble("salario_liquido")));
		}
		if (!achou) {
			System.out.println("Nenhuma folha pendente.");
			return;
		}

		// Confere se a folha escolhida é pendente e da empresa
		System.out.print("Digite o ID da folha (0 para voltar): ");
		int id = sc.nextInt();
		sc.nextLine();
		if (id == 0) {
			return;
		}

		PreparedStatement stmtOk = conn.prepareStatement(
				"SELECT 1 FROM folha_funcionarios ff JOIN funcionarios fu ON fu.id = ff.funcionario_id "
						+ "WHERE ff.id = ? AND ff.status = 'PENDENTE' AND fu.empresa_id = ?");
		stmtOk.setInt(1, id);
		stmtOk.setInt(2, empresaId);
		if (!stmtOk.executeQuery().next()) {
			System.out.println("Folha pendente não encontrada.");
			return;
		}

		System.out.println("1 - Confirmar folha");
		System.out.println("2 - Apagar folha");
		System.out.println("0 - Voltar");
		System.out.print("Escolha: ");
		int opcao = sc.nextInt();
		sc.nextLine();

		if (opcao == 1) {
			// Confirma a folha
			PreparedStatement stmtConf = conn.prepareStatement(
					"UPDATE folha_funcionarios SET status = 'CONFIRMADA' WHERE id = ? AND status = 'PENDENTE'");
			stmtConf.setInt(1, id);
			stmtConf.executeUpdate();
			System.out.println("Folha confirmada com sucesso!");
		} else if (opcao == 2) {
			// Apaga a folha pendente depois de pedir confirmação
			String resposta = Leitor.lerObrigatorio(sc, "Tem certeza que quer apagar? (s/n): ");
			if (resposta.equalsIgnoreCase("s")) {
				PreparedStatement stmtApagar = conn
						.prepareStatement("DELETE FROM folha_funcionarios WHERE id = ? AND status = 'PENDENTE'");
				stmtApagar.setInt(1, id);
				stmtApagar.executeUpdate();
				System.out.println("Folha apagada com sucesso!");
			} else {
				System.out.println("Nada foi apagado.");
			}
		} else if (opcao != 0) {
			System.out.println("Opção inválida.");
		}
	}

	// Calcula e lança a folha de um funcionário (fica PENDENTE até confirmar)
	public static void lancarFolha(Scanner sc, Connection conn, int empresaId) throws SQLException {
		// Lista os funcionários da empresa
		PreparedStatement stmtListar = conn
				.prepareStatement("SELECT id, nome, cargo FROM funcionarios WHERE empresa_id = ?");
		stmtListar.setInt(1, empresaId);
		ResultSet lista = stmtListar.executeQuery();

		System.out.println("Funcionários cadastrados:");
		while (lista.next()) {
			System.out.println(lista.getInt("id") + " - " + lista.getString("nome") + " - " + lista.getString("cargo"));
		}

		System.out.print("Digite o ID do funcionário: ");
		int idEscolhido = sc.nextInt();

		// Busca os dados e o salário do funcionário escolhido
		PreparedStatement stmtBuscar = conn
				.prepareStatement("SELECT nome, cargo, salario_base FROM funcionarios WHERE id = ? AND empresa_id = ?");
		stmtBuscar.setInt(1, idEscolhido);
		stmtBuscar.setInt(2, empresaId);
		ResultSet resultado = stmtBuscar.executeQuery();

		if (!resultado.next()) {
			System.out.println("Funcionário não encontrado.");
			return;
		}

		proventos proventos = new proventos();
		descontos descontos = new descontos();

		proventos.SalarioBase = resultado.getDouble("salario_base");
		proventos.TotalProventos = proventos.SalarioBase;
		String nomeFuncionario = resultado.getString("nome");
		String cargoFuncionario = resultado.getString("cargo");

		// Busca o nome e o CNPJ da empresa para mostrar na folha
		PreparedStatement stmtEmp = conn.prepareStatement("SELECT nome_fantasia, cnpj FROM empresas WHERE id = ?");
		stmtEmp.setInt(1, empresaId);
		ResultSet rsEmp = stmtEmp.executeQuery();
		rsEmp.next();
		String nomeEmpresa = rsEmp.getString("nome_fantasia");
		String cnpjEmpresa = rsEmp.getString("cnpj").replaceAll("(\\d{2})(\\d{3})(\\d{3})(\\d{4})(\\d{2})",
				"$1.$2.$3/$4-$5");

		// Calcula INSS, IRRF e FGTS
		descontos.InssDesconto(proventos.TotalProventos);
		descontos.IrrfDesconto(proventos.TotalProventos, descontos.Inss);
		descontos.FgtsDesconto(proventos.TotalProventos);
		descontos.calcularTotalDescontos();

		// Soma os descontos dos benefícios do funcionário
		double totalBeneficios = 0;
		StringBuilder linhasBeneficios = new StringBuilder();
		NumberFormat moedaBen = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));
		PreparedStatement stmtBen = conn.prepareStatement(
				"SELECT b.nome, fb.desconto_folha FROM funcionarios_beneficios fb JOIN beneficios b ON b.id = fb.beneficio_id WHERE fb.funcionario_id = ? AND fb.ativo = 1 AND fb.desconto_folha > 0");
		stmtBen.setInt(1, idEscolhido);
		ResultSet rsBen = stmtBen.executeQuery();
		while (rsBen.next()) {
			double desc = rsBen.getDouble("desconto_folha");
			totalBeneficios += desc;
			linhasBeneficios.append(rsBen.getString("nome") + ": " + moedaBen.format(desc) + "\n");
		}
		double totalDescontos = descontos.TotalDescontos + totalBeneficios;
		double salarioLiquido = proventos.TotalProventos - totalDescontos;

		// Usa a folha do mês/ano atual, ou cria uma nova se não existir
		LocalDate hoje = LocalDate.now();
		int mes = hoje.getMonthValue();
		int ano = hoje.getYear();
		String dataPagamento = hoje.withDayOfMonth(hoje.lengthOfMonth()).toString();

		int folhaId;
		PreparedStatement stmtBusca = conn
				.prepareStatement("SELECT id FROM folhas_pagamento WHERE mes = ? AND ano = ?");
		stmtBusca.setInt(1, mes);
		stmtBusca.setInt(2, ano);
		ResultSet rsFolha = stmtBusca.executeQuery();

		if (rsFolha.next()) {
			folhaId = rsFolha.getInt("id");
		} else {
			PreparedStatement stmtFolha = conn.prepareStatement(
					"INSERT INTO folhas_pagamento (mes, ano, data_pagamento, status) VALUES (?, ?, ?, ?)",
					Statement.RETURN_GENERATED_KEYS);
			stmtFolha.setInt(1, mes);
			stmtFolha.setInt(2, ano);
			stmtFolha.setString(3, dataPagamento);
			stmtFolha.setString(4, "ABERTA");
			stmtFolha.executeUpdate();
			ResultSet chave = stmtFolha.getGeneratedKeys();
			chave.next();
			folhaId = chave.getInt(1);
		}

		// Não deixa lançar duas vezes o mesmo funcionário no mês
		PreparedStatement stmtJa = conn
				.prepareStatement("SELECT 1 FROM folha_funcionarios WHERE folha_id = ? AND funcionario_id = ?");
		stmtJa.setInt(1, folhaId);
		stmtJa.setInt(2, idEscolhido);
		if (stmtJa.executeQuery().next()) {
			System.out.println("Esse funcionário já foi lançado neste mês. Veja a folha dele na opção 3 (Histórico).");
			return;
		}

		// Salva a folha do funcionário como PENDENTE
		PreparedStatement stmtFolhaFunc = conn.prepareStatement(
				"INSERT INTO folha_funcionarios (folha_id, funcionario_id, salario_base, total_proventos, salario_bruto, base_inss, valor_inss, base_fgts, valor_fgts, base_irpf, valor_irpf, total_descontos, salario_liquido, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDENTE')");
		stmtFolhaFunc.setInt(1, folhaId);
		stmtFolhaFunc.setInt(2, idEscolhido);
		stmtFolhaFunc.setDouble(3, proventos.SalarioBase);
		stmtFolhaFunc.setDouble(4, proventos.TotalProventos);
		stmtFolhaFunc.setDouble(5, proventos.TotalProventos);
		stmtFolhaFunc.setDouble(6, proventos.TotalProventos);
		stmtFolhaFunc.setDouble(7, descontos.Inss);
		stmtFolhaFunc.setDouble(8, proventos.TotalProventos);
		stmtFolhaFunc.setDouble(9, descontos.Fgts);
		stmtFolhaFunc.setDouble(10, proventos.TotalProventos - descontos.Inss);
		stmtFolhaFunc.setDouble(11, descontos.Irrf);
		stmtFolhaFunc.setDouble(12, totalDescontos);
		stmtFolhaFunc.setDouble(13, salarioLiquido);
		stmtFolhaFunc.executeUpdate();

		// Mostra a folha na tela
		NumberFormat moeda = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));

		System.out.println();
		System.out.println("=========== FOLHA DE PAGAMENTO ===========");
		System.out.println("Empresa: " + nomeEmpresa);
		System.out.println("CNPJ: " + cnpjEmpresa);
		System.out.println("Referência: " + String.format("%02d/%d", mes, ano));
		System.out.println("Funcionário: " + nomeFuncionario);
		System.out.println("Cargo: " + cargoFuncionario);
		System.out.println("------------------------------------------");
		System.out.println("PROVENTOS");
		System.out.println("Salário Base:    " + moeda.format(proventos.SalarioBase));
		System.out.println("Total Proventos: " + moeda.format(proventos.TotalProventos));
		System.out.println("------------------------------------------");
		System.out.println("DESCONTOS");
		System.out.println("INSS:            " + moeda.format(descontos.Inss));
		System.out.println("IRRF:            " + moeda.format(descontos.Irrf));
		System.out.print(linhasBeneficios);
		System.out.println("Total descontos: " + moeda.format(totalDescontos));
		System.out.println("------------------------------------------");
		System.out.println("SALÁRIO LÍQUIDO: " + moeda.format(salarioLiquido));
		System.out.println("------------------------------------------");
		System.out.println("FGTS (empresa):  " + moeda.format(descontos.Fgts));
		System.out.println("==========================================");
	}
}
