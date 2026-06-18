package com.finadvise.crm.documents;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/clients/{clientUid}/documents")
@RequiredArgsConstructor
@Tag(name = "Documents", description = "Endpoints for managing client documents and file uploads")
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentUploadOrchestrator orchestrator;

    @Operation(
            summary = "Upload a batch of documents",
            description = "Concurrently uploads multiple files to S3. Requires a metadata JSON map where the keys match the UUID prefix of the uploaded files."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Documents successfully uploaded and saved"),
            @ApiResponse(responseCode = "400", description = "Malformed payload format or missing UUID boundaries", content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied to client or product", content = @Content),
            @ApiResponse(responseCode = "404", description = "Client or Product not found", content = @Content),
            @ApiResponse(responseCode = "415", description = "Unsupported document format or MIME spoofing detected", content = @Content),
            @ApiResponse(responseCode = "422", description = "File is empty or unreadable", content = @Content),
            @ApiResponse(responseCode = "500", description = "Unexpected batch processing failure", content = @Content),
            @ApiResponse(responseCode = "503", description = "S3 Storage currently unavailable", content = @Content)
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<DocumentDTO>> uploadDocuments(
            @Parameter(description = "The unique 8-character UID of the client")
            @PathVariable String clientUid,

            @Parameter(description = "JSON map containing per-file metadata, keyed by frontend-generated UUIDs")
            @RequestPart("metadata") Map<String, FileMetadataDTO> metadataMap,

            @Parameter(description = "List of binary files to upload. Filenames must be prefixed with their matching UUID and delimiter.")
            @RequestPart("files") List<MultipartFile> files,

            Principal principal) {

        log.info("Received batch upload request for clientUid: {} from employeeId: {}. File count: {}",
                clientUid, principal.getName(), files.size());

        List<DocumentDTO> uploadedDocuments = orchestrator.uploadDocumentsBatch(
                clientUid, metadataMap, files, principal.getName()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(uploadedDocuments);
    }

    @Operation(summary = "Get all documents for a client")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of documents retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<ClientDocumentsDTO> getClientDocuments(
            @PathVariable String clientUid,
            Principal principal,
            Pageable pageable) {

        return ResponseEntity.ok(documentService.getClientDocuments(clientUid, principal.getName(), pageable));
    }

    @Operation(summary = "Get a specific document by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Document retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Document not found or access denied", content = @Content)
    })
    @GetMapping("/{documentId}")
    public ResponseEntity<DocumentDTO> getDocumentById(
            @PathVariable String clientUid,
            @PathVariable Long documentId,
            Principal principal) {

        return ResponseEntity.ok(documentService.getDocumentById(clientUid, documentId, principal.getName()));
    }

    @Operation(summary = "Update document metadata", description = "Allows updating the filename or the associated product of an existing document.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Document successfully updated"),
            @ApiResponse(responseCode = "400", description = "Validation error on the request body", content = @Content),
            @ApiResponse(responseCode = "404", description = "Document, Client, or Product not found/access denied", content = @Content)
    })
    @PatchMapping("/{documentId}")
    public ResponseEntity<DocumentDTO> updateDocument(
            @PathVariable String clientUid,
            @PathVariable Long documentId,
            @Valid @RequestBody DocumentUpdateRequestDTO request,
            Principal principal) {

        return ResponseEntity.ok(documentService.updateDocument(clientUid, documentId, request, principal.getName()));
    }

    @Operation(
            summary = "Generate a secure download link",
            description = "Returns a short-lived (60-second) AWS S3 Pre-Signed URL for direct, secure file download."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Secure download link generated successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied to this document", content = @Content),
            @ApiResponse(responseCode = "404", description = "Document or Client not found", content = @Content),
            @ApiResponse(responseCode = "503", description = "S3 Storage currently unavailable", content = @Content)
    })
    @GetMapping("/{documentId}/download")
    public ResponseEntity<Map<String, String>> getDocumentDownloadLink(
            @PathVariable String clientUid,
            @PathVariable Long documentId,
            Principal principal) {

        log.info("Download link requested for documentId: {} by employeeId: {}", documentId, principal.getName());

        String downloadUrl = documentService.generateDownloadUrl(clientUid, documentId, principal.getName());

        // Wrapping the URL in a simple Map to ensure valid JSON output
        return ResponseEntity.ok(Map.of("downloadUrl", downloadUrl));
    }
}
