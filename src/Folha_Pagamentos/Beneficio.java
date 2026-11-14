package Folha_Pagamentos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Scanner;

public class Beneficio {

	// Menu de benefícios: dar, ver, editar e remover
	public static void menuBeneficios(Scanner sc, Connection conn, int empresaId) throws SQLException {
		// Garante que o Vale Combustível existe na lista de benefícios
		PreparedStatement stmtComb = conn.prepareStatement(
				"INSERT OR IGNORE INTO beneficios (nome, descricao, tipo, valor_padrao) VALUES (?, ?, ?, ?)");
		stmtComb.setString(1, "Vale Combustível");
		stmtComb.setString(2, "Benefício destinado ao abastecimento do veículo do funcionário");
		stmtComb.setString(3, "TRANSPORTE");
		stmtComb.setDouble(4, 200.0);
		stmtComb.executeUpdate();

		int opcao = -1;
		while (opcao != 0) {
			System.out.println();
			System.out.println("=========== BENEFÍCIOS ===========");
			System.out.println("1 - Dar benefício a um funcionário");
			System.out.println("2 - Ver benefícios de um funcionário");
			System.out.println("3 - Editar benefício de um funcionário");
			System.out.println("4 - Remover benefício de um funcionário");
			System.out.println("0 - Voltar");
			System.out.print("Escolha: ");
			opcao = sc.nextInt();
			sc.nextLine();

			if (opcao == 1) {
				atribuirBeneficio(sc, conn, empresaId);
			} else if (opcao == 2) {
				listarBeneficios(sc, conn, empresaId);
			} else if (opcao == 3) {
				editarBeneficio(sc, conn, empresaId);
			} else if (opcao == 4) {
				removerBeneficio(sc, conn, empresaId);
			} else if (opcao != 0) {
				System.out.println("Opção inválida.");
			}
		}
	}

	// Dá um benefício a um funcionário
	public static void atribuirBeneficio(Scanner sc, Connection conn, int empresaId) throws SQLException {
		int funcionarioId = FuncionarioMenu.escolherFuncionario(sc, conn, empresaId);
		if (funcionarioId == 0) {
			return;
		}

		NumberFormat moeda = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));

		// Mostra os benefícios disponíveis
		PreparedStatement stmtLista = conn
				.prepareStatement("SELECT id, nome, valor_padrao FROM beneficios WHERE ativo = 1 ORDER BY nome");
		ResultSet lista = stmtLista.executeQuery();
		System.out.println("Benefícios disponíveis:");
		while (lista.next()) {
			System.out.println(lista.getInt("id") + " - " + lista.getString("nome") + " (padrão: "
					+ moeda.format(lista.getDouble("valor_padrao")) + ")");
		}

		// Confere se o benefício escolhido existe
		System.out.print("Digite o ID do benefício (0 para voltar): ");
		int beneficioId = sc.nextInt();
		sc.nextLine();
		if (beneficioId == 0) {
			return;
		}

		PreparedStatement stmtBen = conn.prepareStatement("SELECT nome FROM beneficios WHERE id = ? AND ativo = 1");
		stmtBen.setInt(1, beneficioId);
		ResultSet rsBen = stmtBen.executeQuery();
		if (!rsBen.next()) {
			System.out.println("Benefício não encontrado.");
			return;
		}
		String nomeBeneficio = rsBen.getString("nome");

		// Não deixa dar o mesmo benefício duas vezes
		PreparedStatement stmtJa = conn.prepareStatement(
				"SELECT 1 FROM funcionarios_beneficios WHERE funcionario_id = ? AND beneficio_id = ? AND ativo = 1");
		stmtJa.setInt(1, funcionarioId);
		stmtJa.setInt(2, beneficioId);
		if (stmtJa.executeQuery().next()) {
			System.out.println("Esse funcionário já tem o benefício " + nomeBeneficio + ".");
			return;
		}

		// Pede o valor do benefício
		System.out.print("Valor do benefício: ");
		double valor = sc.nextDouble();
		while (valor < 0) {
			System.out.print("Valor não pode ser negativo. Digite novamente: ");
			valor = sc.nextDouble();
		}

		// Busca o salário do funcionário, para calcular o limite do desconto
		PreparedStatement stmtSal = conn.prepareStatement("SELECT salario_base FROM funcionarios WHERE id = ?");
		stmtSal.setInt(1, funcionarioId);
		ResultSet rsSal = stmtSal.executeQuery();
		rsSal.next();
		double salarioBase = rsSal.getDouble("salario_base");

		// Pede o desconto em % e converte para reais
		double desconto = pedirDesconto(sc, nomeBeneficio, valor, salarioBase);
		sc.nextLine();

		// Salva o benefício do funcionário no banco
		PreparedStatement stmtNovo = conn.prepareStatement(
				"INSERT INTO funcionarios_beneficios (funcionario_id, beneficio_id, valor, desconto_folha, data_inicio) VALUES (?, ?, ?, ?, ?)");
		stmtNovo.setInt(1, funcionarioId);
		stmtNovo.setInt(2, beneficioId);
		stmtNovo.setDouble(3, valor);
		stmtNovo.setDouble(4, desconto);
		stmtNovo.setString(5, LocalDate.now().toString());
		stmtNovo.executeUpdate();

		System.out.println(nomeBeneficio + " adicionado com sucesso! Desconto na folha: " + moeda.format(desconto));
	}

	// Pede o desconto em % (respeitando o limite) e devolve o valor em reais
	public static double pedirDesconto(Scanner sc, String nome, double valor, double salarioBase) {
		double limite = limiteDesconto(nome, valor, salarioBase);
		double base = sobreValor(nome) ? valor : salarioBase;
		String sobre = sobreValor(nome) ? "do valor do benefício" : "do salário";

		if (limite <= 0 || base <= 0) {
			System.out.println("Esse benefício não permite desconto na folha.");
			return 0;
		}

		double maxPct = limite / base * 100;
		System.out.println(String.format("Desconto máximo permitido: %.2f%% %s (R$ %.2f)", maxPct, sobre, limite));
		System.out.print("Desconto em % " + sobre + " (0 se a empresa paga tudo): ");
		double pct = sc.nextDouble();
		while (pct < 0 || pct > maxPct + 0.0001) {
			System.out.print("Percentual inválido. Digite entre 0 e " + String.format("%.2f", maxPct) + ": ");
			pct = sc.nextDouble();
		}
		return Math.round(base * pct / 100 * 100) / 100.0;
	}

	// Mostra os benefícios de um funcionário
	public static void listarBeneficios(Scanner sc, Connection conn, int empresaId) throws SQLException {
		int funcionarioId = FuncionarioMenu.escolherFuncionario(sc, conn, empresaId);
		if (funcionarioId == 0) {
			return;
		}

		PreparedStatement stmt = conn.prepareStatement("SELECT b.nome, fb.valor, fb.desconto_folha, fb.data_inicio "
				+ "FROM funcionarios_beneficios fb " + "JOIN beneficios b ON b.id = fb.beneficio_id "
				+ "WHERE fb.funcionario_id = ? AND fb.ativo = 1 " + "ORDER BY b.nome");
		stmt.setInt(1, funcionarioId);
		ResultSet rs = stmt.executeQuery();

		NumberFormat moeda = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));

		System.out.println();
		System.out.println("=========== BENEFÍCIOS DO FUNCIONÁRIO ===========");
		boolean achou = false;
		while (rs.next()) {
			achou = true;
			System.out.println(rs.getString("nome") + " | Valor: " + moeda.format(rs.getDouble("valor"))
					+ " | Desconto na folha: " + moeda.format(rs.getDouble("desconto_folha")) + " | Desde: "
					+ rs.getString("data_inicio"));
		}
		if (!achou) {
			System.out.println("Nenhum benefício cadastrado.");
		}
		System.out.println("=================================================");
	}

	// Lista os benefícios do funcionário e devolve o ID do escolhido
	public static int escolherBeneficioDoFuncionario(Scanner sc, Connection conn, int funcionarioId)
			throws SQLException {
		PreparedStatement stmt = conn.prepareStatement(
				"SELECT fb.id, b.nome, fb.valor, fb.desconto_folha FROM funcionarios_beneficios fb "
						+ "JOIN beneficios b ON b.id = fb.beneficio_id "
						+ "WHERE fb.funcionario_id = ? AND fb.ativo = 1 ORDER BY b.nome");
		stmt.setInt(1, funcionarioId);
		ResultSet rs = stmt.executeQuery();

		NumberFormat moeda = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));

		System.out.println("Benefícios do funcionário:");
		boolean achou = false;
		while (rs.next()) {
			achou = true;
			System.out.println(rs.getInt("id") + " - " + rs.getString("nome") + " | Valor: "
					+ moeda.format(rs.getDouble("valor")) + " | Desconto: "
					+ moeda.format(rs.getDouble("desconto_folha")));
		}
		if (!achou) {
			System.out.println("Esse funcionário não tem benefícios.");
			return 0;
		}

		// Confere se o benefício escolhido é mesmo do funcionário
		System.out.print("Digite o ID do benefício (0 para voltar): ");
		int id = sc.nextInt();
		sc.nextLine();
		if (id == 0) {
			return 0;
		}

		PreparedStatement stmtOk = conn.prepareStatement(
				"SELECT 1 FROM funcionarios_beneficios WHERE id = ? AND funcionario_id = ? AND ativo = 1");
		stmtOk.setInt(1, id);
		stmtOk.setInt(2, funcionarioId);
		if (!stmtOk.executeQuery().next()) {
			System.out.println("Benefício não encontrado.");
			return 0;
		}
		return id;
	}

	// Diz se o desconto é calculado sobre o valor do benefício (vale refeição e alimentação)
	public static boolean sobreValor(String nomeBeneficio) {
		String nome = nomeBeneficio.toLowerCase();
		return nome.contains("refei") || nome.contains("aliment");
	}

	// Calcula o desconto máximo permitido para cada tipo de benefício
	public static double limiteDesconto(String nomeBeneficio, double valor, double salarioBase) {
		String nome = nomeBeneficio.toLowerCase();
		if (nome.contains("transporte")) {
			return Math.min(salarioBase * 0.06, valor);
		} else if (nome.contains("refei") || nome.contains("aliment")) {
			return valor * 0.20;
		}
		return salarioBase * 0.10;
	}

	// Edita o valor e o desconto de um benefício do funcionário
	public static void editarBeneficio(Scanner sc, Connection conn, int empresaId) throws SQLException {
		int funcionarioId = FuncionarioMenu.escolherFuncionario(sc, conn, empresaId);
		if (funcionarioId == 0) {
			return;
		}
		int fbId = escolherBeneficioDoFuncionario(sc, conn, funcionarioId);
		if (fbId == 0) {
			return;
		}

		// Busca o nome do benefício e o salário, para calcular o limite
		PreparedStatement stmtDados = conn.prepareStatement(
				"SELECT b.nome, f.salario_base FROM funcionarios_beneficios fb "
						+ "JOIN beneficios b ON b.id = fb.beneficio_id "
						+ "JOIN funcionarios f ON f.id = fb.funcionario_id WHERE fb.id = ?");
		stmtDados.setInt(1, fbId);
		ResultSet dados = stmtDados.executeQuery();
		dados.next();
		String nome = dados.getString("nome");
		double salarioBase = dados.getDouble("salario_base");

		// Pede o novo valor e o desconto em %
		System.out.print("Novo valor do benefício (R$): ");
		double valor = sc.nextDouble();
		while (valor < 0) {
			System.out.print("Valor não pode ser negativo. Digite novamente: ");
			valor = sc.nextDouble();
		}

		double desconto = pedirDesconto(sc, nome, valor, salarioBase);
		sc.nextLine();

		// Salva as alterações no banco
		PreparedStatement stmt = conn.prepareStatement(
				"UPDATE funcionarios_beneficios SET valor = ?, desconto_folha = ? WHERE id = ? AND funcionario_id = ?");
		stmt.setDouble(1, valor);
		stmt.setDouble(2, desconto);
		stmt.setInt(3, fbId);
		stmt.setInt(4, funcionarioId);
		stmt.executeUpdate();

		NumberFormat moeda = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));
		System.out.println(nome + " atualizado! Desconto na folha: " + moeda.format(desconto));
	}

	// Remove um benefício do funcionário (só desativa, não apaga do banco)
	public static void removerBeneficio(Scanner sc, Connection conn, int empresaId) throws SQLException {
		int funcionarioId = FuncionarioMenu.escolherFuncionario(sc, conn, empresaId);
		if (funcionarioId == 0) {
			return;
		}
		int fbId = escolherBeneficioDoFuncionario(sc, conn, funcionarioId);
		if (fbId == 0) {
			return;
		}

		String resposta = Leitor.lerObrigatorio(sc, "Tem certeza que quer remover? (s/n): ");
		if (resposta.equalsIgnoreCase("s")) {
			PreparedStatement stmt = conn.prepareStatement(
					"UPDATE funcionarios_beneficios SET ativo = 0 WHERE id = ? AND funcionario_id = ?");
			stmt.setInt(1, fbId);
			stmt.setInt(2, funcionarioId);
			stmt.executeUpdate();
			System.out.println("Benefício removido com sucesso!");
		} else {
			System.out.println("Nada foi removido.");
		}
	}
}