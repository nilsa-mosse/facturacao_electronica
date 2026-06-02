package ao.co.hzconsultoria.efacturacao.service;

import ao.co.hzconsultoria.efacturacao.model.EscalaoIrt;
import ao.co.hzconsultoria.efacturacao.model.Empresa;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class IrtImportService {

    public List<EscalaoIrt> parseFile(MultipartFile file, Empresa empresa) throws Exception {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("Nome do ficheiro inválido.");
        }

        if (filename.toLowerCase().endsWith(".pdf")) {
            return parsePdf(file.getInputStream(), empresa);
        } else if (filename.toLowerCase().endsWith(".xls") || filename.toLowerCase().endsWith(".xlsx")) {
            return parseExcel(file.getInputStream(), empresa);
        } else {
            throw new IllegalArgumentException("Formato de ficheiro não suportado. Por favor, envie um PDF ou Excel.");
        }
    }

    private List<EscalaoIrt> parsePdf(InputStream inputStream, Empresa empresa) throws Exception {
        List<EscalaoIrt> escaloes = new ArrayList<>();

        try (PDDocument document = PDDocument.load(inputStream)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            String[] lines = text.split("\\r?\\n");

            for (String line : lines) {
                EscalaoIrt escalao = parseLine(line, empresa);
                if (escalao != null) {
                    escaloes.add(escalao);
                }
            }
        }

        return escaloes;
    }

    private List<EscalaoIrt> parseExcel(InputStream inputStream, Empresa empresa) throws Exception {
        List<EscalaoIrt> escaloes = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            for (Row row : sheet) {
                // Concatena as células com espaço para simular uma linha de texto
                StringBuilder lineText = new StringBuilder();
                for (Cell cell : row) {
                    String value = formatter.formatCellValue(cell);
                    if (value != null && !value.trim().isEmpty()) {
                        lineText.append(value).append(" ");
                    }
                }

                EscalaoIrt escalao = parseLine(lineText.toString(), empresa);
                if (escalao != null) {
                    escaloes.add(escalao);
                }
            }
        }

        return escaloes;
    }

    private EscalaoIrt parseLine(String line, Empresa empresa) {
        if (line == null || line.trim().isEmpty()) {
            return null;
        }

        // Ignorar linhas de cabeçalho comuns
        String lowerLine = line.toLowerCase();
        if (lowerLine.contains("tabela do irt") || lowerLine.contains("grupo de")
                || lowerLine.contains("limite inferior")) {
            return null;
        }

        // Tokenizar por espaços e extrair números
        String[] tokens = line.split("\\s+");
        List<String> numericTokens = new ArrayList<>();

        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i].trim();
            // Verifica se o token contem dígitos
            if (token.matches(".*\\d+.*")) {
                // Ignorar índices ordinais como "1º", "2º", "13º", "1.º" no início
                if (token.matches("^\\d+(\\.|º|ª|o|a)*$") && i < 2) {
                    continue;
                }
                numericTokens.add(token);
            }
        }

        if (numericTokens.isEmpty()) {
            return null;
        }

        // Converter tokens para valores double
        List<Double> numbers = new ArrayList<>();
        for (String t : numericTokens) {
            double num = parsePortugueseNumber(t);
            numbers.add(num);
        }

        // Casos de acordo com a quantidade de números identificados na linha
        double inf = 0.0;
        Double sup = null;
        double parcela = 0.0;
        double taxa = 0.0;

        if (numbers.size() == 1) {
            // Provavelmente o primeiro escalão: Até 70.000
            sup = numbers.get(0);
        } else if (numbers.size() == 3) {
            // Último escalão: De 10.000.001, parcela fixa 2.342.250, taxa 25%
            inf = numbers.get(0);
            parcela = numbers.get(1);
            taxa = numbers.get(2);
            sup = null; // Sem limite superior
        } else if (numbers.size() >= 4) {
            // Escalões intermédios: De 70.001 A 100.000, parcela 3.000, taxa 10%
            inf = numbers.get(0);
            sup = numbers.get(1);
            parcela = numbers.get(2);
            taxa = numbers.get(3);
        } else {
            return null;
        }

        // Se a taxa for maior que 100, provavelmente há erro de leitura, mas limitamos
        if (taxa > 100.0) {
            taxa = taxa / 100.0; // caso venha expresso como proporção mal interpretada
        }

        return new EscalaoIrt(empresa, inf, sup, parcela, taxa);
    }

    private double parsePortugueseNumber(String valStr) {
        if (valStr == null)
            return 0.0;
        valStr = valStr.trim()
                .replaceAll("\\s+", "") // remove espaços
                .replace("Kz", "") // remove símbolos de moeda
                .replace("kz", "")
                .replace("%", "");
        if (valStr.isEmpty() || valStr.equals("-") || valStr.equals("—")) {
            return 0.0;
        }

        if (valStr.contains(",") && valStr.contains(".")) {
            valStr = valStr.replace(".", "").replace(",", ".");
        } else if (valStr.contains(",")) {
            valStr = valStr.replace(",", ".");
        } else if (valStr.contains(".")) {
            int dotIndex = valStr.indexOf('.');
            int lastDotIndex = valStr.lastIndexOf('.');
            if (dotIndex != lastDotIndex) {
                valStr = valStr.replace(".", "");
            } else {
                String afterDot = valStr.substring(dotIndex + 1);
                if (afterDot.length() == 3) {
                    valStr = valStr.replace(".", "");
                }
            }
        }

        try {
            return Double.parseDouble(valStr);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
