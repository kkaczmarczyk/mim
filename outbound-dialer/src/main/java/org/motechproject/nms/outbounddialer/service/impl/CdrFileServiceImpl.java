package org.motechproject.nms.outbounddialer.service.impl;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.motechproject.alerts.contract.AlertService;
import org.motechproject.alerts.domain.AlertStatus;
import org.motechproject.alerts.domain.AlertType;
import org.motechproject.nms.outbounddialer.domain.AuditRecord;
import org.motechproject.nms.outbounddialer.domain.CallDetailRecord;
import org.motechproject.nms.outbounddialer.domain.FileType;
import org.motechproject.nms.outbounddialer.repository.CallDetailRecordDataService;
import org.motechproject.nms.outbounddialer.repository.FileAuditDataService;
import org.motechproject.nms.outbounddialer.service.CdrFileService;
import org.motechproject.nms.outbounddialer.web.contract.CdrFileNotificationRequest;
import org.motechproject.server.config.SettingsFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


/**
 * Implementation of the {@link CdrFileService} interface.
 */
@Service("cdrFileService")
public class CdrFileServiceImpl implements CdrFileService {

    private static final String CDR_FILE_DIRECTORY = "outbound-dialer.cdr_file_directory";

    private static final Logger LOGGER = LoggerFactory.getLogger(CdrFileServiceImpl.class);

    private SettingsFacade settingsFacade;
    private FileAuditDataService fileAuditDataService;
    private AlertService alertService;
    private CallDetailRecordDataService cdrDataService;



    @Autowired
    public CdrFileServiceImpl(@Qualifier("outboundDialerSettings") SettingsFacade settingsFacade,
                              FileAuditDataService fileAuditDataService, AlertService alertService,
                              CallDetailRecordDataService cdrDataService) {
        this.settingsFacade = settingsFacade;
        this.fileAuditDataService = fileAuditDataService;
        this.alertService = alertService;
        this.cdrDataService = cdrDataService;
    }


    private List<CallDetailRecord> readCdrs(String cdrFileLocation, String summaryFileName) {
        File userDirectory = new File(System.getProperty("user.home"));
        File cdrDirectory = new File(userDirectory, cdrFileLocation);
        File cdrSummary = new File(cdrDirectory, summaryFileName);
        List<CallDetailRecord> lines = new ArrayList<>();
        try {
            LineIterator it = FileUtils.lineIterator(cdrSummary);
            while (it.hasNext()) {
                lines.add(CallDetailRecord.fromLine(it.nextLine()));
            }
        } catch (IOException e) {
            String error = String.format("Unable to read cdrSummary file %s: %s", cdrSummary, e.getMessage());
            LOGGER.error(error);
            alertService.create(cdrSummary.toString(), "cdrSummary", error, AlertType.CRITICAL, AlertStatus.NEW, 0,
                    null);
            //todo: what do I want to do with the identifier field here?
            fileAuditDataService.create(new AuditRecord(null, FileType.CDR_FILE, cdrSummary.toString(), error, null,
                    null));
            throw new IllegalStateException(error);
        }

        LOGGER.info("Successfully read {} cdrSummary lines", lines.size());

        return lines;
    }


    @Override
    public void processCdrFile(CdrFileNotificationRequest request) {
        final String cdrFileLocation = settingsFacade.getProperty(CDR_FILE_DIRECTORY);
        LOGGER.debug("Processing {} located in {}", request, cdrFileLocation);

        //todo: audit this request

        //read the summary file and keep it in memory - so we can easily refer to it when we process the cdrDetail file
        List<CallDetailRecord> cdrs = readCdrs(cdrFileLocation, request.getCdrSummary().getCdrFile());

        //for now, and hopefully for ever, only process the summary file
        for (int lineNumber = 1; lineNumber < cdrs.size(); lineNumber++) {
            cdrDataService.create(cdrs.get(lineNumber - 1));
        }

        //todo: add recordCount, think about checksum
        String fileIdentifier = UUID.randomUUID().toString();
        fileAuditDataService.create(new AuditRecord(fileIdentifier, FileType.CDR_FILE, request.getFileName(), null,
                null, "Success"));

    }
}
