package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.model.Categoria;
import ao.co.hzconsultoria.efacturacao.model.Empresa;
import ao.co.hzconsultoria.efacturacao.repository.CategoriaRepository;
import ao.co.hzconsultoria.efacturacao.repository.EmpresaRepository;
import ao.co.hzconsultoria.efacturacao.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private EmpresaRepository empresaRepository;

    @GetMapping
    public ResponseEntity<List<Categoria>> listar() {
        Long empresaId = SecurityUtils.getCurrentEmpresaId();
        if (empresaId == null) {
            return ResponseEntity.status(401).build();
        }
        List<Categoria> categorias = categoriaRepository.findByEmpresa_Id(empresaId);
        return ResponseEntity.ok(categorias);
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
            return ResponseEntity.badRequest().body("Já existe uma categoria com o nome '" + categoria.getNome() + "' nesta empresa.");
        }

        try {
            categoria.setEmpresa(empresa);
            Categoria salva = categoriaRepository.save(categoria);
            System.out.println(">>> Categoria '" + salva.getNome() + "' salva com sucesso ID: " + salva.getId());
            return ResponseEntity.ok(salva);
        } catch (Exception e) {
            System.err.println(">>> ERRO AO GRAVAR CATEGORIA: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Erro interno ao gravar: " + e.getMessage());
        }
    }
}
