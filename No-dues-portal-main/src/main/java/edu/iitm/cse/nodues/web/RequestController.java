package edu.iitm.cse.nodues.web;

import edu.iitm.cse.nodues.domain.NoDuesRequest;
import edu.iitm.cse.nodues.domain.UserRole;
import edu.iitm.cse.nodues.repository.FacultyAssignmentRepository;
import edu.iitm.cse.nodues.repository.NoDuesRequestRepository;
import edu.iitm.cse.nodues.repository.RequestHistoryRepository;
import edu.iitm.cse.nodues.repository.RequestVersionRepository;
import edu.iitm.cse.nodues.repository.UserRepository;
import edu.iitm.cse.nodues.service.RequestWorkflowService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import java.security.Principal;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import edu.iitm.cse.nodues.domain.RequestFile;
import edu.iitm.cse.nodues.service.RequestManagementService;

@Controller
public class RequestController {

    private final RequestWorkflowService workflowService;
    private final NoDuesRequestRepository requestRepository;
    private final RequestHistoryRepository historyRepository;
    private final RequestVersionRepository versionRepository;
    private final FacultyAssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    private final edu.iitm.cse.nodues.service.RequestFileService fileService;
    private final RequestManagementService managementService;

    public RequestController(
            RequestWorkflowService workflowService,
            NoDuesRequestRepository requestRepository,
            RequestHistoryRepository historyRepository,
            RequestVersionRepository versionRepository,
            FacultyAssignmentRepository assignmentRepository,
            UserRepository userRepository,
            edu.iitm.cse.nodues.service.RequestFileService fileService,
            RequestManagementService managementService) {
        this.workflowService = workflowService;
        this.requestRepository = requestRepository;
        this.historyRepository = historyRepository;
        this.versionRepository = versionRepository;
        this.assignmentRepository = assignmentRepository;
        this.userRepository = userRepository;
        this.fileService = fileService;
        this.managementService = managementService;
    }

    @GetMapping("/requests")
    public String requests(Principal principal, Model model) {
        var user = userRepository.findByEmailIgnoreCase(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Account was not found"));
        var requests = switch (user.getRole()) {
            case ADMIN, STAFF -> requestRepository.findAll();
            case STUDENT -> requestRepository.findByStudentIdOrderByUpdatedAtDesc(user.getId());
            case FACULTY -> assignmentRepository.findByFacultyUserId(user.getId()).stream()
                    .map(assignment -> requestRepository.findById(assignment.getRequestId()).orElse(null))
                    .filter(java.util.Objects::nonNull)
                    .toList();
        };
        var archivedRequestIds = managementService.archivedRequestIds(user.getId());
        model.addAttribute("requests", requests.stream()
                .filter(request -> !archivedRequestIds.contains(request.getId()))
                .toList());
        model.addAttribute("usersById", userRepository.findAll().stream()
                .collect(Collectors.toMap(edu.iitm.cse.nodues.domain.User::getId,
                        Function.identity(), (first, second) -> first)));
        model.addAttribute("requestOwner", user.getRole() == UserRole.STUDENT ? "My requests"
                : user.getRole() == UserRole.FACULTY ? "Assigned requests" : "All requests");
        model.addAttribute("archivedRequestIds", archivedRequestIds);
        return "requests/list";
    }

    @GetMapping("/requests/archive")
    public String archivedRequests(Principal principal, Model model) {
        var user = userRepository.findByEmailIgnoreCase(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Account was not found"));
        var requests = switch (user.getRole()) {
            case ADMIN, STAFF -> requestRepository.findAll();
            case STUDENT -> requestRepository.findByStudentIdOrderByUpdatedAtDesc(user.getId());
            case FACULTY -> assignmentRepository.findByFacultyUserId(user.getId()).stream()
                    .map(assignment -> requestRepository.findById(assignment.getRequestId()).orElse(null))
                    .filter(java.util.Objects::nonNull).toList();
        };
        model.addAttribute("requests", managementService.archivedRequests(user.getId(), requests));
        model.addAttribute("usersById", userRepository.findAll().stream()
                .collect(Collectors.toMap(edu.iitm.cse.nodues.domain.User::getId,
                        Function.identity(), (first, second) -> first)));
        model.addAttribute("requestOwner", "Archived requests");
        model.addAttribute("archivedRequestIds", managementService.archivedRequestIds(user.getId()));
        return "requests/list";
    }

    @GetMapping("/requests/new")
    public String newRequest(Principal principal, Model model) {
        var user = userRepository.findByEmailIgnoreCase(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Account was not found"));
        if (user.getRole() != UserRole.STUDENT) {
            throw new IllegalStateException("Only Students can create requests");
        }
        model.addAttribute("requestForm", new RequestForm());
        model.addAttribute("faculty", userRepository.findAll().stream()
                .filter(facultyUser -> facultyUser.getRole() == UserRole.FACULTY && facultyUser.isActive())
                .toList());
        return "requests/new";
    }

    @PostMapping("/requests")
    public String createRequest(
            @Valid @ModelAttribute("requestForm") RequestForm form,
            BindingResult bindingResult,
            Principal principal,
            @org.springframework.web.bind.annotation.RequestParam(name = "files", required = false)
                    MultipartFile[] files,
            Model model) {
        if (bindingResult.hasErrors()) {
            loadUsers(model);
            return "requests/new";
        }
        try {
            String studentId = userRepository.findByEmailIgnoreCase(principal.getName())
                    .filter(user -> user.getRole() == UserRole.STUDENT)
                    .orElseThrow(() -> new IllegalStateException("Only Students can create requests"))
                    .getId();
            NoDuesRequest request = workflowService.createRequest(
                    studentId, form.getFacultyAdvisorId(), form.getDetails());
            fileService.storeStudentFiles(request.getId(), studentId,
                    files == null ? java.util.List.of() : Arrays.asList(files));
            return "redirect:/requests/" + request.getId();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            model.addAttribute("error", exception.getMessage());
            loadUsers(model);
            return "requests/new";
        }
    }

    @GetMapping("/requests/{requestId}")
    public String requestDetails(
            @PathVariable String requestId,
            Principal principal,
            Model model) {
        NoDuesRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request was not found"));
        var user = userRepository.findByEmailIgnoreCase(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Account was not found"));
        boolean allowed = switch (user.getRole()) {
            case ADMIN, STAFF -> true;
            case STUDENT -> request.getStudentId().equals(user.getId());
            case FACULTY -> assignmentRepository.findByRequestIdAndFacultyUserId(requestId, user.getId())
                    .isPresent();
        };
        if (!allowed) {
            throw new IllegalArgumentException("You do not have access to this request");
        }
        model.addAttribute("request", request);
        model.addAttribute("versions", versionRepository.findByRequestIdOrderByVersionNumberAsc(requestId));
        model.addAttribute("assignments",
                assignmentRepository.findByRequestIdOrderByDisplayPositionAsc(requestId));
        model.addAttribute("history", historyRepository.findByRequestIdOrderByOccurredAtAsc(requestId));
        model.addAttribute("usersById", userRepository.findAll().stream()
                .collect(Collectors.toMap(edu.iitm.cse.nodues.domain.User::getId,
                        Function.identity(), (first, second) -> first)));
        model.addAttribute("currentUser", user);
        model.addAttribute("files", fileService.listFiles(requestId));
        model.addAttribute("faculty", userRepository.findAll().stream()
                .filter(found -> found.getRole() == UserRole.FACULTY && found.isActive())
                .toList());
        return "requests/detail";
    }

    @GetMapping("/requests/{requestId}/files/{fileId}")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable String requestId,
            @PathVariable String fileId,
            Principal principal) {
        ensureRequestAccess(requestId, principal);
        RequestFile file = fileService.listFiles(requestId).stream()
                .filter(found -> found.getId().equals(fileId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("File was not found"));
        Resource resource = fileService.loadFile(fileId, requestId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(file.getOriginalFileName())
                                .build().toString())
                .body(resource);
    }

    @PostMapping("/requests/{requestId}/archive")
    public String archive(@PathVariable String requestId, Principal principal) {
        managementService.archive(currentUserId(principal), requestId);
        return "redirect:/requests";
    }

    @PostMapping("/requests/{requestId}/unarchive")
    public String unarchive(@PathVariable String requestId, Principal principal) {
        managementService.unarchive(currentUserId(principal), requestId);
        return "redirect:/requests";
    }

    private void ensureRequestAccess(String requestId, Principal principal) {
        NoDuesRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request was not found"));
        var user = userRepository.findByEmailIgnoreCase(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Account was not found"));
        boolean allowed = switch (user.getRole()) {
            case ADMIN, STAFF -> true;
            case STUDENT -> request.getStudentId().equals(user.getId());
            case FACULTY -> assignmentRepository
                    .findByRequestIdAndFacultyUserId(requestId, user.getId()).isPresent();
        };
        if (!allowed) {
            throw new IllegalArgumentException("You do not have access to this request");
        }
    }

    @PostMapping("/requests/{requestId}/faculty/approve")
    public String approve(
            @PathVariable String requestId,
            Principal principal,
            @RequestParam(required = false) String remark) {
        workflowService.approveRequest(requestId, currentUserId(principal), remark);
        return "redirect:/requests/" + requestId;
    }

    @PostMapping("/requests/{requestId}/faculty/reject")
    public String reject(
            @PathVariable String requestId,
            Principal principal,
            @RequestParam String reason) {
        workflowService.rejectRequest(requestId, currentUserId(principal), reason);
        return "redirect:/requests/" + requestId;
    }

    @PostMapping("/requests/{requestId}/faculty/withdraw")
    public String withdraw(@PathVariable String requestId, Principal principal) {
        workflowService.withdrawDecision(requestId, currentUserId(principal));
        return "redirect:/requests/" + requestId;
    }

    @PostMapping("/requests/{requestId}/forward")
    public String forward(
            @PathVariable String requestId,
            Principal principal,
            @RequestParam(name = "facultyIds", required = false) java.util.List<String> facultyIds) {
        workflowService.forwardRequest(requestId, currentUserId(principal),
                facultyIds == null ? java.util.List.of() : facultyIds);
        return "redirect:/requests/" + requestId;
    }

    @PostMapping("/requests/{requestId}/close")
    public String close(@PathVariable String requestId, Principal principal) {
        workflowService.closeRequest(requestId, currentUserId(principal));
        return "redirect:/requests/" + requestId;
    }

    @PostMapping("/requests/{requestId}/certificate")
    public String certificate(
            @PathVariable String requestId,
            Principal principal,
            @RequestParam MultipartFile file) {
        fileService.storeCertificateFile(requestId, currentUserId(principal), file);
        return "redirect:/requests/" + requestId;
    }

    @PostMapping("/requests/{requestId}/delete")
    public String delete(@PathVariable String requestId, Principal principal) {
        var user = userRepository.findByEmailIgnoreCase(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Account was not found"));
        if (user.getRole() == UserRole.ADMIN) {
            managementService.deleteByAdmin(user.getId(), requestId);
        } else if (user.getRole() == UserRole.STAFF) {
            managementService.deleteByStaff(user.getId(), requestId);
        } else {
            throw new IllegalArgumentException("Only Staff or Admin can delete requests");
        }
        return "redirect:/requests";
    }

    @PostMapping("/requests/{requestId}/restore")
    public String restore(@PathVariable String requestId, Principal principal) {
        managementService.restoreByDeletingStaff(currentUserId(principal), requestId);
        return "redirect:/requests/" + requestId;
    }

    @PostMapping("/requests/{requestId}/resubmit")
    public String resubmit(
            @PathVariable String requestId,
            Principal principal,
            @RequestParam String details) {
        workflowService.resubmitRequest(requestId, currentUserId(principal), details);
        return "redirect:/requests/" + requestId;
    }

    String currentUserId(Principal principal) {
        return userRepository.findByEmailIgnoreCase(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Account was not found"))
                .getId();
    }

    private void loadUsers(Model model) {
        model.addAttribute("faculty", userRepository.findAll().stream()
                .filter(user -> user.getRole() == UserRole.FACULTY && user.isActive())
                .toList());
    }
}
