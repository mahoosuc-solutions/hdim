package com.healthdata.fhir.bulk;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BulkExportServiceExceptionTest {

    @Test
    void shouldExposeExportExceptions() {
        BulkExportService.ExportLimitExceededException limit = new BulkExportService.ExportLimitExceededException("limit");
        BulkExportService.ExportJobNotFoundException notFound = new BulkExportService.ExportJobNotFoundException("job-1");

        assertThat(limit.getMessage()).isEqualTo("limit");
        assertThat(notFound.getMessage()).isEqualTo("Export job not found: job-1");
    }
}
