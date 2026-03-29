package ao.co.hzconsultoria.efacturacao.controller;

import ao.co.hzconsultoria.efacturacao.model.Despesa;
import ao.co.hzconsultoria.efacturacao.service.DespesaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.util.UUID;

@Controller
@RequestMapping("/despesas")
public class DespesaController {

    @Autowired
    private DespesaService despesaService;

    @Autowired
    private MessageSource messageSource;

    private final String UPLOAD_DIR = "src/main/resources/static/uploads/despesas/";

    // Listar despesas
    @GetMapping("/listar")
    public String listarDespesas(Model model) {
        model.addAttribute("despesas", despesaService.listarTodas());
        return "listarDespesas";
    }

    // Exibir formulário de cadastro
    @GetMapping("/adicionar")
    public String mostrarFormularioCadastro(Model model) {
        model.addAttribute("despesa", new Despesa());
        return "cadastroDespesa";
    }

    // Processar cadastro
    @PostMapping("/adicionar")
    public String cadastrarDespesa(@ModelAttribute Despesa despesa, 
                                   @RequestParam("fileFatura") MultipartFile file,
                                   RedirectAttributes redirectAttributes) {
        tratarUpload(despesa, file);
        despesaService.salvar(despesa);
        redirectAttributes.addFlashAttribute("mensagem", messageSource.getMessage("msg.despesa.salvo", null, LocaleContextHolder.getLocale()));
        return "redirect:/despesas/listar";
    }

    // Exibir formulário de edição
    @GetMapping("/editar/{id}")
    public String mostrarFormularioEdicao(@PathVariable Long id, Model model) {
        Despesa despesa = despesaService.buscarPorId(id);
        model.addAttribute("despesa", despesa);
        return "cadastroDespesa";
    }

    // Atualizar despesa
    @PostMapping("/atualizar")
    public String atualizarDespesa(@ModelAttribute Despesa despesa, 
                                   @RequestParam("fileFatura") MultipartFile file,
                                   RedirectAttributes redirectAttributes) {
        tratarUpload(despesa, file);
        despesaService.atualizar(despesa);
        redirectAttributes.addFlashAttribute("mensagem", messageSource.getMessage("msg.despesa.atualizada", null, LocaleContextHolder.getLocale()));
        return "redirect:/despesas/listar";
    }

    private void tratarUpload(Despesa despesa, MultipartFile file) {
        if (!file.isEmpty()) {
            try {
                Path uploadPath = Paths.get(UPLOAD_DIR);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
                Path filePath = uploadPath.resolve(fileName);
                Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

                despesa.setFaturaPath(fileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadFatura(@PathVariable Long id) {
        Despesa despesa = despesaService.buscarPorId(id);
        if (despesa != null && despesa.getFaturaPath() != null) {
            try {
                Path filePath = Paths.get(UPLOAD_DIR).resolve(despesa.getFaturaPath());
                Resource resource = new UrlResource(filePath.toUri());

                if (resource.exists()) {
                    return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_OCTET_STREAM)
                            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + despesa.getFaturaPath() + "\"")
                            .body(resource);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return ResponseEntity.notFound().build();
    }

    // Excluir despesa
    @GetMapping("/excluir/{id}")
    public String excluirDespesa(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        despesaService.excluir(id);
        redirectAttributes.addFlashAttribute("mensagem", messageSource.getMessage("msg.despesa.apagada", null, LocaleContextHolder.getLocale()));
        return "redirect:/despesas/listar";
    }
}
