package edu.iitm.cse.nodues.service;

import edu.iitm.cse.nodues.domain.NoDuesRequest;
import edu.iitm.cse.nodues.domain.RequestFile;
import edu.iitm.cse.nodues.domain.RequestFileType;
import edu.iitm.cse.nodues.domain.RequestStatus;
import edu.iitm.cse.nodues.domain.UserRole;
import edu.iitm.cse.nodues.repository.NoDuesRequestRepository;
import edu.iitm.cse.nodues.repository.RequestFileRepository;
import edu.iitm.cse.nodues.repository.UserRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class RequestFileService {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private static final int MAX_STUDENT_FILES = 3;

    private final RequestFileRepository fileRepository;
    private final NoDuesRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final Path storageRoot;

    public RequestFileService(
            RequestFileRepository fileRepository,
            NoDuesRequestRepository requestRepository,
            UserRepository userRepository,
            @Value("${app.file-storage-path:data/uploads}") String storagePath) {
        this.fileRepository = fileRepository;
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
        this.storageRoot = Paths.get(storagePath).toAbsolutePath().normalize();
    }

    @Transactional
    public void storeStudentFiles(
            String requestId,
            String studentId,
            List<MultipartFile> files) {
        List<MultipartFile> validFiles = files == null
                ? List.of()
                : files.stream().filter(file -> file != null && !file.isEmpty()).toList();
        if (validFiles.size() > MAX_STUDENT_FILES) {
            throw new IllegalArgumentException("A request version can have at most three files");
        }
        validFiles.forEach(file -> {
            if (file.getSize() > MAX_FILE_SIZE) {
                throw new IllegalArgumentException("Each file must be 5 MB or smaller");
            }
        });
        validFiles.forEach(file -> storeStudentFile(requestId, studentId, file));
    }

    @Transactional
    public RequestFile storeStudentFile(
            String requestId,
            String studentId,
            MultipartFile file) {
        NoDuesRequest request = getRequest(requestId);
        requireStudent(request, studentId);
        if (request.getStatus() != RequestStatus.WAITING_FOR_STAFF
                && request.getStatus() != RequestStatus.REVISION_REQUESTED) {
            throw new IllegalStateException("Student files cannot be changed after submission");
        }
        List<RequestFile> currentFiles = fileRepository.findByRequestIdAndVersionNumberAndFileType(
                requestId, request.getCurrentVersionNumber(), RequestFileType.STUDENT_REQUEST);
        if (currentFiles.size() >= MAX_STUDENT_FILES) {
            throw new IllegalArgumentException("A request version can have at most three files");
        }
        return store(request, studentId, file, RequestFileType.STUDENT_REQUEST);
    }

    @Transactional
    public RequestFile storeCertificateFile(
            String requestId,
            String staffId,
            MultipartFile file) {
        NoDuesRequest request = getRequest(requestId);
        if (!staffId.equals(request.getAssignedStaffId())) {
            throw new IllegalArgumentException("Only the assigned Staff member can add a certificate");
        }

        if (request.getStatus() != RequestStatus.AWAITING_STAFF_CLOSURE) {
            throw new IllegalStateException("Certificate can only be added during closure");
        }
        List<RequestFile> certificates = fileRepository.findByRequestIdAndVersionNumberAndFileType(
                requestId, request.getCurrentVersionNumber(), RequestFileType.CERTIFICATE);
        if (!certificates.isEmpty()) {
            throw new IllegalArgumentException("Only one certificate file is allowed");
        }
        return store(request, staffId, file, RequestFileType.CERTIFICATE);
    }

    @Transactional(readOnly = true)
    public List<RequestFile> listFiles(String requestId) {
        return fileRepository.findByRequestId(requestId);
    }

    @Transactional(readOnly = true)
    public Resource loadFile(String fileId, String requestId) {
        RequestFile file = fileRepository.findById(fileId)
                .filter(found -> found.getRequestId().equals(requestId))
                .orElseThrow(() -> new IllegalArgumentException("File was not found"));
        Path path = storageRoot.resolve(file.getStorageReference()).normalize();
        if (!path.startsWith(storageRoot) || !Files.isRegularFile(path)) {
            throw new IllegalArgumentException("Stored file was not found");
        }
        return new FileSystemResource(path);
    }

    private RequestFile store(
            NoDuesRequest request,
            String uploaderId,
            MultipartFile file,
            RequestFileType fileType) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("A non-empty file is required");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Each file must be 5 MB or smaller");
        }
        String originalName = sanitizeName(file.getOriginalFilename());
        String storedName = UUID.randomUUID() + "-" + originalName;
        Path requestDirectory = storageRoot.resolve(request.getId())
                .resolve(String.valueOf(request.getCurrentVersionNumber()));
        Path target = requestDirectory.resolve(storedName).normalize();
        if (!target.startsWith(storageRoot)) {
            throw new IllegalArgumentException("Invalid file name");
        }
        try {
            Files.createDirectories(requestDirectory);
            file.transferTo(target);
        } catch (IOException e) {
            throw new IllegalStateException("Could not store the uploaded file", e);
        }
        return fileRepository.save(new RequestFile(
                request.getId(),
                request.getCurrentVersionNumber(),
                fileType,
                uploaderId,
                originalName,
                file.getSize(),
                storageRoot.relativize(target).toString()));
    }

    private NoDuesRequest getRequest(String requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request was not found"));
    }

    private void requireStudent(NoDuesRequest request, String studentId) {
        if (!request.getStudentId().equals(studentId)
                || userRepository.findById(studentId)
                        .filter(user -> user.isActive() && user.getRole() == UserRole.STUDENT)
                        .isEmpty()) {
            throw new IllegalArgumentException("Only the request student can upload request files");
        }
    }

    private static String sanitizeName(String originalName) {
        String name = originalName == null ? "upload.bin" : Paths.get(originalName).getFileName().toString();
        if (name.isBlank() || name.equals(".") || name.equals("..")) {
            return "upload.bin";
        }
        return name;
    }
}
