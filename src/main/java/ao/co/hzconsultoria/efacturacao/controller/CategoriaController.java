package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.model.Categoria;
import ao.co.hzconsultoria.efacturacao.model.Empresa;
import ao.co.hzconsultoria.efacturacao.repository.CategoriaRepository;
import ao.co.hzconsultoria.efacturacao.repository.EmpresaRepository;
import ao.co.hzconsultoria.efacturacao.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private EmpresaRepository empresaRepository;

    // ── Utilitário: converter Categoria em DTO simples (evita referência circular) ──
    private Map<String, Object> toDto(Categoria c) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", c.getId());
        dto.put("nome", c.getNome());
        return dto;
    }

    @GetMapping
    public ResponseEntity<?> listar() {
        Long empresaId = SecurityUtils.getCurrentEmpresaId();
        if (empresaId == null) {
            return ResponseEntity.status(401).build();
        }
        List<Map<String, Object>> dtos = categoriaRepository.findByEmpresa_Id(empresaId)
                .stream().map(this::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping
    public ResponseEntity<?> cadastrar(@RequestBody Categoria categoria) {
        System.out.println(">>> Iniciando cadastro de categoria: " + categoria.getNome());

        Long empresaId = SecurityUtils.getCurrentEmpresaId();
        if (empresaId == null) {
            System.err.println(">>> Erro: EmpresaId não encontrado na sessão.");
            return ResponseEntity.status(401).body("Utilizador não tem empresa associada.");
        }

        Empresa empresa = empresaRepository.findById(empresaId).orElse(null);
        if (empresa == null) {
            System.err.println(">>> Erro: Empresa com ID " + empresaId + " não existe no banco.");
            return ResponseEntity.badRequest().body("Empresa não encontrada no sistema.");
        }

        // Validar duplicado
        Categoria existente = categoriaRepository.findByNomeAndEmpresa_Id(categoria.getNome(), empresaId);
        if (existente != null) {
            return ResponseEntity.badRequest()
                    .body("Já existe uma categoria com o nome '" + categoria.getNome() + "' nesta empresa.");
        }

        try {
            categoria.setEmpresa(empresa);
            Categoria salva = categoriaRepository.save(categoria);
            System.out.println(">>> Categoria '" + salva.getNome() + "' salva com sucesso ID: " + salva.getId());
            return ResponseEntity.ok(toDto(salva));
        } catch (Exception e) {
            System.err.println(">>> ERRO AO GRAVAR CATEGORIA: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Erro interno ao gravar: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> editar(@PathVariable Long id, @RequestBody Categoria categoriaAtualizada) {
        Long empresaId = SecurityUtils.getCurrentEmpresaId();
        if (empresaId == null) {
            return ResponseEntity.status(401).body("Utilizador não tem empresa associada.");
        }

        Categoria categoria = categoriaRepository.findById(id).orElse(null);
        if (categoria == null || !categoria.getEmpresa().getId().equals(empresaId)) {
            return ResponseEntity.notFound().build();
        }

        // Verificar duplicado (excluindo o próprio registo)
        Categoria existente = categoriaRepository.findByNomeAndEmpresa_Id(categoriaAtualizada.getNome(), empresaId);
        if (existente != null && !existente.getId().equals(id)) {
            return ResponseEntity.badRequest()
                    .body("Já existe uma categoria com o nome '" + categoriaAtualizada.getNome() + "' nesta empresa.");
        }

        try {
            categoria.setNome(categoriaAtualizada.getNome());
            Categoria salva = categoriaRepository.save(categoria);
            return ResponseEntity.ok(toDto(salva));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erro ao atualizar categoria: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        Long empresaId = SecurityUtils.getCurrentEmpresaId();
        if (empresaId == null) {
            return ResponseEntity.status(401).body("Utilizador não tem empresa associada.");
        }

        Categoria categoria = categoriaRepository.findById(id).orElse(null);
        if (categoria == null || !categoria.getEmpresa().getId().equals(empresaId)) {
            return ResponseEntity.notFound().build();
        }

        try {
            categoriaRepository.delete(categoria);
            return ResponseEntity.ok().build();
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.badRequest()
                    .body("Não é possível eliminar esta categoria porque existem produtos associados a ela.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erro ao eliminar a categoria: " + e.getMessage());
        }
    }
}
