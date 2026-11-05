package br.com.fiap.postech.msexpedicao.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

class MdcRequestFilterTest {

    private final MdcRequestFilter filter = new MdcRequestFilter();

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void should_propagate_correlation_id() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader("X-Correlation-Id", "corr-1");
        var response = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);
        doAnswer(invocation -> {
            assertThat(MDC.get("correlation_id")).isEqualTo("corr-1");
            return null;
        }).when(chain).doFilter(any(), any());

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader("X-Correlation-Id")).isEqualTo("corr-1");
    }

    @Test
    void should_generate_correlation_id_when_absent() throws Exception {
        var response = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);
        doAnswer(invocation -> {
            assertThat(MDC.get("correlation_id")).isNotBlank();
            return null;
        }).when(chain).doFilter(any(), any());

        filter.doFilter(new MockHttpServletRequest(), response, chain);

        assertThat(response.getHeader("X-Correlation-Id")).isNotBlank();
    }

    @Test
    void should_propagate_branch_id_and_clear_mdc_on_error() throws Exception {
        var request = new MockHttpServletRequest();
        request.setAttribute("branchId", 7L);
        var chain = mock(FilterChain.class);
        doThrow(new ServletException("boom")).when(chain).doFilter(any(), any());

        assertThatThrownBy(() -> filter.doFilter(request, new MockHttpServletResponse(), chain))
                .isInstanceOf(ServletException.class);

        assertThat(MDC.get("branch_id")).isNull();
        assertThat(MDC.get("correlation_id")).isNull();
    }
}
