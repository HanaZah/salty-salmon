package com.finadvise.crm.documents;

import com.finadvise.crm.clients.ClientService;
import com.finadvise.crm.common.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentUploadOrchestrator {

    private final DocumentService documentService;
    private final ClientService clientService;

    @Qualifier("ioExecutor")
    private final Executor ioExecutor;

    private record FileUploadPair(MultipartFile file, FileMetadataDTO metadata) {}

    public List<DocumentDTO> uploadDocumentsBatch(String clientUid,
                                                  Map<String, FileMetadataDTO> metadataMap,
                                                  List<MultipartFile> files,
                                                  String employeeId) {

        // 1. Client Lookup (Main thread)
        Long clientId = clientService.getClientId(clientUid);

        // 2. Pre-Pairing (Main thread)
        List<FileUploadPair> pairs = pairFilesWithMetadata(files, metadataMap);

        // 3. FAN-OUT: Dispatch to worker threads
        List<CompletableFuture<DocumentUploadResultDTO>> uploadFutures = pairs.stream()
                .map(pair -> CompletableFuture.supplyAsync(() ->
                                documentService.uploadDocument(clientUid, employeeId, pair.metadata(), pair.file()),
                        ioExecutor
                ))
                .toList();

        List<DocumentUploadResultDTO> processedFiles;

        // 4. THE BARRIER: Strictly scoped try-catch for concurrent execution
        try {
            CompletableFuture.allOf(uploadFutures.toArray(new CompletableFuture[0])).join();

            // FAN-IN: Harvest the results
            processedFiles = uploadFutures.stream()
                    .map(CompletableFuture::join)
                    .toList();

        } catch (CompletionException e) {
            log.error("Batch upload failed during concurrent processing. Unwrapping exact cause...", e.getCause());
            // Extract the actual domain error (e.g. DmsUnavailableException) and wrap it safely
            throw new DocumentBatchProcessingException("Failed to process document batch securely", e.getCause());
        }

        // 5. Batch Database Persist (Safely back on the main thread, outside the try-catch)
        return documentService.saveAllDocuments(clientId, processedFiles);
    }

    private List<FileUploadPair> pairFilesWithMetadata(List<MultipartFile> files,
                                                       Map<String, FileMetadataDTO> metadataMap) {
        List<FileUploadPair> pairedList = new ArrayList<>();

        for (MultipartFile file : files) {
            String rawName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "";
            String[] parts = rawName.split(Constants.DOCUMENT_UPLOAD_FILENAME_DELIMITER, 2);

            if (parts.length < 2) {
                throw new MalformedDocumentPayloadException(
                        "Invalid file payload format. Missing metadata mapping identifier for file: " + rawName
                );
            }

            String fileId = parts[0];
            FileMetadataDTO metadata = metadataMap.get(fileId);

            if (metadata == null) {
                throw new MalformedDocumentPayloadException(
                        "Orphaned file detected. No metadata found for mapping ID: " + fileId
                );
            }

            pairedList.add(new FileUploadPair(file, metadata));
        }

        return pairedList;
    }
}
