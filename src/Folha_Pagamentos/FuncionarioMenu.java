package Folha_Pagamentos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Scanner;

public class FuncionarioMenu {

	// Duração da experiência em meses (para mudar o prazo, troque só este número)
	private static final int MESES_EXPERIENCIA = 3;

	// Lista os funcionários da empresa e devolve o ID do escolhido (0 se não achar)
	public static int escolherFuncionario(Scanner sc, Connection conn, int empresaId) throws SQLException {
		PreparedStatement stmt = conn.prepareStatement("SELECT id, nome FROM funcionarios WHERE empresa_id = ?");
		stmt.setInt(1, empresaId);
		ResultSet rs = stmt.executeQuery();

		System.out.println("Funcionários:");
		boolean achou = false;
		while (rs.next()) {
			achou = true;
			System.out.println(rs.getInt("id") + " - " + rs.getString("nome"));
		}
		if (!achou) {
			System.out.println("Nenhum funcionário cadastrado.");
			return 0;
		}

		// Confere se o ID digitado é de um funcionário dessa empresa
		System.out.print("Digite o ID do funcionário (0 para voltar): ");
		int id = sc.nextInt();
		sc.nextLine();
		if (id == 0) {
			return 0;
		}

		PreparedStatement stmtOk = conn.prepareStatement("SELECT 1 FROM funcionarios WHERE id = ? AND empresa_id = ?");
		stmtOk.setInt(1, id);
		stmtOk.setInt(2, empresaId);
		if (!stmtOk.executeQuery().next()) {
			System.out.println("Funcionário não encontrado.");
			return 0;
		}
		return id;
	}

	// Altera o cargo ou o salário base de um funcionário
	public static void editarFuncionario(Scanner sc, Connection conn, int empresaId) throws SQLException {
		int funcionarioId = escolherFuncionario(sc, conn, empresaId);
		if (funcionarioId == 0) {
			return;
		}

		System.out.println();
		System.out.println("1 - Alterar cargo");
		System.out.println("2 - Alterar salário base");
		System.out.println("0 - Voltar");
		System.out.print("Escolha: ");
		int opcao = sc.nextInt();
		sc.nextLine();

		if (opcao == 1) {
			// Altera o cargo
			String novoCargo = Leitor.lerObrigatorio(sc, "Novo cargo: ");
			PreparedStatement stmt = conn
					.prepareStatement("UPDATE funcionarios SET cargo = ? WHERE id = ? AND empresa_id = ?");
			stmt.setString(1, novoCargo);
			stmt.setInt(2, funcionarioId);
			stmt.setInt(3, empresaId);
			stmt.executeUpdate();
			System.out.println("Cargo atualizado com sucesso!");
		} else if (opcao == 2) {
			// Altera o salário base (não aceita valor negativo)
			System.out.print("Novo salário base: ");
			double novoSalario = sc.nextDouble();
			while (novoSalario < 0) {
				System.out.print("Salário não pode ser negativo. Digite novamente: ");
				novoSalario = sc.nextDouble();
			}
			sc.nextLine();
			PreparedStatement stmt = conn
					.prepareStatement("UPDATE funcionarios SET salario_base = ? WHERE id = ? AND empresa_id = ?");
			stmt.setDouble(1, novoSalario);
			stmt.setInt(2, funcionarioId);
			stmt.setInt(3, empresaId);
			stmt.executeUpdate();
			System.out.println("Salário atualizado com sucesso!");
		} else if (opcao != 0) {
			System.out.println("Opção inválida.");
		}
	}

	// Cria a coluna email na tabela funcionarios se ela ainda não existir
	public static void garantirColunaEmail(Connection conn) throws SQLException {
		PreparedStatement stmt = conn.prepareStatement("PRAGMA table_info(funcionarios)");
		ResultSet rs = stmt.executeQuery();
		boolean existe = false;
		while (rs.next()) {
			if (rs.getString("name").equalsIgnoreCase("email")) {
				existe = true;
			}
		}
		if (!existe) {
			conn.createStatement().executeUpdate("ALTER TABLE funcionarios ADD COLUMN email TEXT");
		}
	}

	// Cadastra um funcionário novo na empresa
	public static void cadastrarFuncionario(Scanner sc, Connection conn, int empresaId) throws SQLException {

		funcionario funcionario = new funcionario();

		// Garante que o banco tem a coluna email
		garantirColunaEmail(conn);

		// Pede os dados pessoais (CPF e email são validados)
		System.out.print("Nome: ");
		funcionario.Nome = sc.nextLine();

		funcionario.Cpf = Leitor.lerCpf(sc);

		funcionario.Email = Leitor.lerEmail(sc, "Email do funcionário: ");

		System.out.print("Data de nascimento (ddmmaaaa): ");
		String textoNascimento = sc.nextLine();
		funcionario.DataNascimento = LocalDate.of(Integer.parseInt(textoNascimento.substring(4, 8)),
				Integer.parseInt(textoNascimento.substring(2, 4)), Integer.parseInt(textoNascimento.substring(0, 2)));

		System.out.print("Data de admissao (ddmmaaaa): ");
		String textoAdmissao = sc.nextLine();
		funcionario.DataAdmissao = LocalDate.of(Integer.parseInt(textoAdmissao.substring(4, 8)),
				Integer.parseInt(textoAdmissao.substring(2, 4)), Integer.parseInt(textoAdmissao.substring(0, 2)));

		// Pede os dados do trabalho
		System.out.print("Cargo: ");
		funcionario.Cargo = sc.nextLine();

		System.out.print("Salário Base: ");
		double salarioBase = sc.nextDouble();
		while (salarioBase < 0) {
			System.out.print("Salário não pode ser negativo. Digite novamente: ");
			salarioBase = sc.nextDouble();
		}

		// Salva o funcionário no banco
		String sql = "INSERT INTO funcionarios (nome, cpf, email, data_nascimento, data_admissao, cargo, salario_base, empresa_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
		PreparedStatement stmt = conn.prepareStatement(sql);
		stmt.setString(1, funcionario.Nome);
		stmt.setString(2, funcionario.Cpf);
		stmt.setString(3, funcionario.Email);
		stmt.setString(4, funcionario.DataNascimento.toString());
		stmt.setString(5, funcionario.DataAdmissao.toString());
		stmt.setString(6, funcionario.Cargo);
		stmt.setDouble(7, salarioBase);
		stmt.setInt(8, empresaId);
		stmt.executeUpdate();

		System.out.println("Funcionário cadastrado com sucesso!");
	}

	// Menu de funcionários: dados de um ou lista de todos
	public static void menuFuncionarios(Scanner sc, Connection conn, int empresaId) throws SQLException {
		int opcao = -1;
		while (opcao != 0) {
			System.out.println();
			System.out.println("=========== FUNCIONÁRIOS ===========");
			System.out.println("1 - Dados de um funcionário");
			System.out.println("2 - Ver todos os funcionários");
			System.out.println("0 - Voltar");
			System.out.print("Escolha: ");
			opcao = sc.nextInt();
			sc.nextLine();

			if (opcao == 1) {
				verFuncionario(sc, conn, empresaId);
			} else if (opcao == 2) {
				listarFuncionarios(sc, conn, empresaId);
			} else if (opcao != 0) {
				System.out.println("Opção inválida.");
			}
		}
	}

	// Mostra os dados de um funcionário da empresa
	public static void verFuncionario(Scanner sc, Connection conn, int empresaId) throws SQLException {
		int funcionarioId = escolherFuncionario(sc, conn, empresaId);
		if (funcionarioId == 0) {
			return;
		}

		// Garante que o banco tem a coluna email antes de buscar
		garantirColunaEmail(conn);

		PreparedStatement stmt = conn.prepareStatement(
				"SELECT nome, cpf, email, data_nascimento, data_admissao, cargo, salario_base "
						+ "FROM funcionarios WHERE id = ? AND empresa_id = ?");
		stmt.setInt(1, funcionarioId);
		stmt.setInt(2, empresaId);
		ResultSet rs = stmt.executeQuery();
		rs.next();

		// Formata o CPF, o salário e as datas para mostrar na tela
		String cpf = rs.getString("cpf").replaceAll("(\\d{3})(\\d{3})(\\d{3})(\\d{2})", "$1.$2.$3-$4");
		String email = rs.getString("email");
		NumberFormat moeda = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));
		DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM/yyyy");
		LocalDate nascimento = LocalDate.parse(rs.getString("data_nascimento"));
		LocalDate admissao = LocalDate.parse(rs.getString("data_admissao"));

		System.out.println();
		System.out.println("=========== DADOS DO FUNCIONÁRIO ===========");
		System.out.println("Nome: " + rs.getString("nome"));
		System.out.println("CPF: " + cpf);
		System.out.println("Email: " + (email == null || email.isEmpty() ? "Não informado" : email));
		System.out.println("Nascimento: " + nascimento.format(formato));
		System.out.println("Admissão: " + admissao.format(formato));
		System.out.println("Tempo de empresa: " + tempoDeEmpresa(admissao));
		System.out.println("Cargo: " + rs.getString("cargo"));
		System.out.println("Salário base: " + moeda.format(rs.getDouble("salario_base")));
		System.out.println("============================================");
	}

	// Lista os funcionários com tempo de empresa e situação da experiência
	public static void listarFuncionarios(Scanner sc, Connection conn, int empresaId) throws SQLException {
		System.out.println();
		System.out.println("1 - Todos os funcionários");
		System.out.println("2 - Só quem está em experiência");
		System.out.println("3 - Só quem já passou da experiência");
		System.out.println("0 - Voltar");
		System.out.print("Escolha: ");
		int filtro = sc.nextInt();
		sc.nextLine();
		if (filtro == 0) {
			return;
		}
		if (filtro < 1 || filtro > 3) {
			System.out.println("Opção inválida.");
			return;
		}

		PreparedStatement stmt = conn.prepareStatement(
				"SELECT nome, cargo, data_admissao FROM funcionarios WHERE empresa_id = ? ORDER BY nome");
		stmt.setInt(1, empresaId);
		ResultSet rs = stmt.executeQuery();

		DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM/yyyy");
		LocalDate hoje = LocalDate.now();
		int qtd = 0;
		int totalExperiencia = 0;
		int totalFora = 0;

		System.out.println();
		System.out.println("==================== FUNCIONÁRIOS ====================");
		System.out.println("Período de experiência: " + MESES_EXPERIENCIA + " meses");
		System.out.println("======================================================");

		while (rs.next()) {
			LocalDate admissao = LocalDate.parse(rs.getString("data_admissao"));
			LocalDate fim = admissao.plusMonths(MESES_EXPERIENCIA);
			boolean emExperiencia = hoje.isBefore(fim);

			// Conta todos, para o resumo do final
			if (emExperiencia) {
				totalExperiencia++;
			} else {
				totalFora++;
			}

			// Aplica o filtro escolhido
			if (filtro == 2 && !emExperiencia) {
				continue;
			}
			if (filtro == 3 && emExperiencia) {
				continue;
			}

			// Mostra os dados do funcionário
			qtd++;
			System.out.println();
			System.out.println("Nome:             " + rs.getString("nome"));
			System.out.println("Cargo:            " + rs.getString("cargo"));
			System.out.println("Admissão:         " + admissao.format(formato));
			System.out.println("Tempo de empresa: " + tempoDeEmpresa(admissao));
			if (emExperiencia) {
				long faltam = ChronoUnit.DAYS.between(hoje, fim);
				System.out.println("Situação:         EM EXPERIÊNCIA (termina em " + fim.format(formato) + ", faltam "
						+ faltam + " dia(s))");
			} else {
				System.out.println("Situação:         FORA DA EXPERIÊNCIA (passou em " + fim.format(formato) + ")");
			}
			System.out.println("------------------------------------------------------");
		}

		if (qtd == 0) {
			System.out.println();
			System.out.println("Nenhum funcionário encontrado.");
		}

		// Resumo com todos os funcionários da empresa
		System.out.println();
		System.out.println("Em experiência: " + totalExperiencia + " | Fora da experiência: " + totalFora);
		System.out.println("======================================================");
	}

	// Calcula há quanto tempo a pessoa está na empresa (só mostra o que não é zero)
	public static String tempoDeEmpresa(LocalDate admissao) {
		Period p = Period.between(admissao, LocalDate.now());
		if (p.isNegative()) {
			return "ainda não começou";
		}

		ArrayList<String> partes = new ArrayList<>();
		if (p.getYears() > 0) {
			partes.add(p.getYears() + (p.getYears() == 1 ? " ano" : " anos"));
		}
		if (p.getMonths() > 0) {
			partes.add(p.getMonths() + (p.getMonths() == 1 ? " mês" : " meses"));
		}
		if (p.getDays() > 0) {
			partes.add(p.getDays() + (p.getDays() == 1 ? " dia" : " dias"));
		}

		if (partes.isEmpty()) {
			return "começou hoje";
		}

		// Junta as partes: "1 ano, 2 meses e 3 dias"
		String ultimo = partes.remove(partes.size() - 1);
		return partes.isEmpty() ? ultimo : String.join(", ", partes) + " e " + ultimo;
	}
}