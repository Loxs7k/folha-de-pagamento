package Folha_Pagamentos;

public class descontos {

	double Inss;
    double Irrf;
    double Fgts;
    double irpf;
    public double TotalDescontos;
    
    //===================== CALCULO INSS =====================
    public double InssDesconto(double base) {
        double[] teto = {1621.00, 2902.84, 4354.27, 8475.55};
        double[] taxa = {0.075, 0.09, 0.12, 0.14};
        double inss = 0, anterior = 0;
        for (int i = 0; i < teto.length; i++) {
            double faixa = Math.min(base, teto[i]) - anterior;
            if (faixa <= 0) break;
            inss += faixa * taxa[i];
            anterior = teto[i];
        }
        Inss = Math.round(inss * 100.0) / 100.0;
        return Inss;
    }
    
    //===================== CALCULO IRRF =====================
    public double IrrfDesconto(double totalProventos, double Inss) {
        double base = totalProventos - Inss;
        double imposto;

        if (base <= 2428.80)      imposto = 0;
        else if (base <= 2826.65) imposto = base * 0.075 - 182.16;
        else if (base <= 3751.05) imposto = base * 0.15  - 394.16;
        else if (base <= 4664.68) imposto = base * 0.225 - 675.49;
        else                      imposto = base * 0.275 - 908.73;

        // ===================== REGRAS DE 2026: ISENTO ATE 5.000 E REDUÇAO GRADUAL ATE 7.350 =====================
        if (totalProventos <= 5000.00) {
            imposto = 0;
        } else if (totalProventos <= 7350.00) {
            imposto -= (978.62 - 0.133145 * totalProventos);
        }

        Irrf = Math.max(0, Math.round(imposto * 100.0) / 100.0);
        return Irrf;
    }
    
    //===================== FGTS DESCONTO =====================
    public double FgtsDesconto(double totalProventos) {

        Fgts = totalProventos * 0.08;

        return Fgts;
    }
    
    //===================== SOMA =====================
    public double calcularTotalDescontos() {
        TotalDescontos = Inss + Irrf;
        return TotalDescontos;
    }
    
    
    
}
