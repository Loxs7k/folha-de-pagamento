package Folha_Pagamentos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Scanner;

public class Historico {

	// Consulta base do histórico (cada método acrescenta o seu WHERE)
	private static final String SQL_HISTORICO = "SELECT e.nome_fantasia, e.cnpj, fp.mes, fp.ano, fu.nome, fu.cargo, "
			+ "ff.salario_base, ff.total_proventos, ff.valor_inss, ff.valor_irpf, ff.total_descontos, "
			+ "ff.salario_liquido, ff.valor_fgts, ff.status FROM folha_funcionarios ff "
			+ "JOIN folhas_pagamento fp ON fp.id = ff.folha_id " + "JOIN funcionarios fu ON fu.id = ff.funcionario_id "
			+ "JOIN empresas e ON e.id = fu.empresa_id ";

	// Menu do histórico: por funcionário ou por mês
	public static void menuHistorico(Scanner sc, Connection conn, int empresaId) throws SQLException {
		int opcao = -1;
		while (opcao != 0) {
			System.out.println();
			System.out.println("=========== HISTÓRICO DE FOLHAS ===========");
			System.out.println("1 - Histórico de um funcionário");
			System.out.println("2 - Histórico do mês da empresa");
			System.out.println("0 - Voltar");
			System.out.print("Escolha: ");
			opcao = sc.nextInt();
			sc.nextLine();

			if (opcao == 1) {
				historicoFuncionario(sc, conn, empresaId);
			} else if (opcao == 2) {
				historicoMes(sc, conn, empresaId);
			} else if (opcao != 0) {
				System.out.println("Opção inválida.");
			}
		}
	}

	// Mostra todas as folhas de um funcionário
	public static void historicoFuncionario(Scanner sc, Connection conn, int empresaId) throws SQLException {
		int funcionarioId = FuncionarioMenu.escolherFuncionario(sc, conn, empresaId);
		if (funcionarioId == 0) {
			return;
		}

		PreparedStatement stmt = conn.prepareStatement(
				SQL_HISTORICO + "WHERE fu.id = ? AND fu.empresa_id = ? ORDER BY fp.ano DESC, fp.mes DESC");
		stmt.setInt(1, funcionarioId);
		stmt.setInt(2, empresaId);
		ResultSet rs = stmt.executeQuery();

		double[] totais = imprimirFolhas(rs);
		if (totais[0] == 0) {
			System.out.println("Nenhuma folha lançada para esse funcionário.");
		}
	}

	// Mostra as folhas de um mês e os totais da empresa
	public static void historicoMes(Scanner sc, Connection conn, int empresaId) throws SQLException {
		// Pede o mês e o ano
		System.out.print("Mês (1 a 12): ");
		int mes = sc.nextInt();
		while (mes < 1 || mes > 12) {
			System.out.print("Mês inválido. Digite de 1 a 12: ");
			mes = sc.nextInt();
		}
		System.out.print("Ano (ex: 2026): ");
		int ano = sc.nextInt();
		sc.nextLine();

		PreparedStatement stmt = conn.prepareStatement(
				SQL_HISTORICO + "WHERE fp.mes = ? AND fp.ano = ? AND fu.empresa_id = ? ORDER BY fu.nome");
		stmt.setInt(1, mes);
		stmt.setInt(2, ano);
		stmt.setInt(3, empresaId);
		ResultSet rs = stmt.executeQuery();

		double[] totais = imprimirFolhas(rs);
		if (totais[0] == 0) {
			System.out.println("Nenhuma folha lançada em " + String.format("%02d/%d", mes, ano) + ".");
			return;
		}

		// Mostra o total do mês
		NumberFormat moeda = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));
		System.out.println();
		System.out.println("=========== TOTAL DE " + String.format("%02d/%d", mes, ano) + " ===========");
		System.out.println("Folhas:          " + (int) totais[0]);
		System.out.println("Total proventos: " + moeda.format(totais[1]));
		System.out.println("Total descontos: " + moeda.format(totais[2]));
		System.out.println("Total líquido:   " + moeda.format(totais[3]));
		System.out.println("==========================================");
	}

	// Imprime cada folha e devolve [quantidade, proventos, descontos, líquido]
	public static double[] imprimirFolhas(ResultSet rs) throws SQLException {
		NumberFormat moeda = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));
		double qtd = 0, proventos = 0, descontos = 0, liquido = 0;

		while (rs.next()) {
			// Soma os totais
			qtd++;
			proventos += rs.getDouble("total_proventos");
			descontos += rs.getDouble("total_descontos");
			liquido += rs.getDouble("salario_liquido");

			// Formata o CNPJ e calcula o total de benefícios descontados
			String cnpj = rs.getString("cnpj").replaceAll("(\\d{2})(\\d{3})(\\d{3})(\\d{4})(\\d{2})", "$1.$2.$3/$4-$5");
			double beneficios = rs.getDouble("total_descontos") - rs.getDouble("valor_inss")
					- rs.getDouble("valor_irpf");

			// Mostra a folha
			System.out.println();
			System.out.println("=========== FOLHA DE PAGAMENTO ===========");
			System.out.println("Empresa: " + rs.getString("nome_fantasia"));
			System.out.println("CNPJ: " + cnpj);
			System.out.println("Referência: " + String.format("%02d/%d", rs.getInt("mes"), rs.getInt("ano")));
			System.out.println("Status: " + rs.getString("status"));
			System.out.println("Funcionário: " + rs.getString("nome"));
			System.out.println("Cargo: " + rs.getString("cargo"));
			System.out.println("------------------------------------------");
			System.out.println("PROVENTOS");
			System.out.println("Salário Base:    " + moeda.format(rs.getDouble("salario_base")));
			System.out.println("Total Proventos: " + moeda.format(rs.getDouble("total_proventos")));
			System.out.println("------------------------------------------");
			System.out.println("DESCONTOS");
			System.out.println("INSS:            " + moeda.format(rs.getDouble("valor_inss")));
			System.out.println("IRRF:            " + moeda.format(rs.getDouble("valor_irpf")));
			if (beneficios > 0.005) {
				System.out.println("Benefícios:      " + moeda.format(beneficios));
			}
			System.out.println("Total descontos: " + moeda.format(rs.getDouble("total_descontos")));
			System.out.println("------------------------------------------");
			System.out.println("SALÁRIO LÍQUIDO: " + moeda.format(rs.getDouble("salario_liquido")));
			System.out.println("------------------------------------------");
			System.out.println("FGTS (empresa):  " + moeda.format(rs.getDouble("valor_fgts")));
			System.out.println("==========================================");
		}
		return new double[] { qtd, proventos, descontos, liquido };
	}
}
