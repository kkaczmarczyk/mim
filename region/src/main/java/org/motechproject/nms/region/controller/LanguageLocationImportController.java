package org.motechproject.nms.region.controller;

import org.motechproject.alerts.contract.AlertService;
import org.motechproject.alerts.domain.AlertStatus;
import org.motechproject.alerts.domain.AlertType;
import org.motechproject.nms.csv.exception.CsvImportException;
import org.motechproject.nms.csv.service.CsvAuditService;
import org.motechproject.nms.region.csv.LanguageLocationImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.InputStreamReader;

@Controller
public class LanguageLocationImportController {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocationDataImportController.class);

    private AlertService alertService;
    private LanguageLocationImportService languageLocationImportService;
    private CsvAuditService csvAuditService;


    @RequestMapping(value = "/languageLocationCode/import", method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.OK)
    public void importLanguageLocationCodes(@RequestParam MultipartFile csvFile) {
        try {
            try (InputStream in = csvFile.getInputStream()) {
                languageLocationImportService.importData(new InputStreamReader(in));
                csvAuditService.auditSuccess(csvFile.getOriginalFilename(), "/region/languageLocationCode/import");
            }
        } catch (CsvImportException e) {
            logError(csvFile.getOriginalFilename(), "/region/languageLocationCode/import", e);
            throw e;
        } catch (Exception e) {
            logError(csvFile.getOriginalFilename(), "/region/languageLocationCode/import", e);
            throw new CsvImportException("An error occurred during CSV import", e);
        }
    }

    private void logError(String fileName, String endpoint, Exception exception) {
        LOGGER.error(exception.getMessage(), exception);
        csvAuditService.auditFailure(fileName, endpoint, exception.getMessage());
        alertService.create("language_location_codes_import_error", "Language location codes import error",
                exception.getMessage(), AlertType.CRITICAL, AlertStatus.NEW, 0, null);
    }

    @Autowired
    public void setAlertService(AlertService alertService) {
        this.alertService = alertService;
    }

    @Autowired
    public void setLanguageLocationImportService(LanguageLocationImportService languageLocationImportService) {
        this.languageLocationImportService = languageLocationImportService;
    }

    @Autowired
    public void setCsvAuditService(CsvAuditService csvAuditService) {
        this.csvAuditService = csvAuditService;
    }
}
