package ao.co.hzconsultoria.efacturacao.service;

import ao.co.hzconsultoria.efacturacao.model.Categoria;
import ao.co.hzconsultoria.efacturacao.model.Empresa;
import ao.co.hzconsultoria.efacturacao.repository.CategoriaRepository;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class CategoriaImportService {

    @Autowired
    private CategoriaRepository categoriaRepository;

    public ImportResult importCategorias(MultipartFile file, Empresa empresa) throws Exception {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("Nome do ficheiro inválido.");
        }

        List<String> names = new ArrayList<>();
        if (filename.toLowerCase().endsWith(".xls") || filename.toLowerCase().endsWith(".xlsx")) {
            names = parseExcel(file.getInputStream());
        } else if (filename.toLowerCase().endsWith(".csv") || filename.toLowerCase().endsWith(".txt")) {
            names = parseCsvOrTxt(file.getInputStream());
        } else {
            throw new IllegalArgumentException("Formato de ficheiro não suportado. Envie Excel (.xls/.xlsx), CSV ou TXT.");
        }

        int importedCount = 0;
        int duplicateCount = 0;

        for (String name : names) {
            String trimmedName = name.trim();
            if (trimmedName.isEmpty()) {
                continue;
            }

            Categoria existente = categoriaRepository.findByNomeAndEmpresa_Id(trimmedName, empresa.getId());
            if (existente == null) {
                Categoria nova = new Categoria();
                nova.setNome(trimmedName);
                nova.setEmpresa(empresa);
                categoriaRepository.save(nova);
                importedCount++;
            } else {
                duplicateCount++;
            }
        }

        return new ImportResult(importedCount, duplicateCount);
    }

    private List<String> parseExcel(InputStream inputStream) throws Exception {
        List<String> names = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            for (Row row : sheet) {
                // Lê a primeira célula preenchida da linha
                Cell cell = row.getCell(0);
                if (cell != null) {
                    String val = formatter.formatCellValue(cell).trim();
                    // Evita cabeçalhos comuns
                    if (!val.isEmpty() && !val.equalsIgnoreCase("categoria") && !val.equalsIgnoreCase("nome") && !val.equalsIgnoreCase("categorias")) {
                        names.add(val);
                    }
                }
            }
        }
        return names;
    }

    private List<String> parseCsvOrTxt(InputStream inputStream) throws Exception {
        List<String> names = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                // Se for CSV, pode ter cabeçalho ou separadores.
                // Mas geralmente, se for apenas uma lista de categorias, será uma coluna.
                if (line.equalsIgnoreCase("categoria") || line.equalsIgnoreCase("nome") || line.equalsIgnoreCase("categorias")) {
                    continue;
                }
                
                // Se a linha contiver vírgula ou ponto e vírgula, e for um CSV com mais campos (ex: "id,nome"), vamos extrair a coluna do nome.
                if (line.contains(",")) {
                    String[] parts = line.split(",");
                    if (parts.length > 1) {
                        // Se a primeira parte parecer um ID numérico, a segunda parte é provavelmente o nome
                        if (parts[0].trim().matches("^\\d+$")) {
                            names.add(parts[1].trim());
                        } else {
                            names.add(parts[0].trim());
                        }
                    } else {
                        names.add(line);
                    }
                } else if (line.contains(";")) {
                    String[] parts = line.split(";");
                    if (parts.length > 1) {
                        if (parts[0].trim().matches("^\\d+$")) {
                            names.add(parts[1].trim());
                        } else {
                            names.add(parts[0].trim());
                        }
                    } else {
                        names.add(line);
                    }
                } else {
                    names.add(line);
                }
            }
        }
        return names;
    }

    public static class ImportResult {
        private final int imported;
        private final int duplicates;

        public ImportResult(int imported, int duplicates) {
            this.imported = imported;
            this.duplicates = duplicates;
        }

        public int getImported() { return imported; }
        public int getDuplicates() { return duplicates; }
    }
}
