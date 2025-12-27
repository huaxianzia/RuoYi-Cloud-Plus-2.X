package org.dromara.gateway.utils;

import cn.hutool.core.util.ObjectUtil;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.gateway.filter.WebCacheRequestFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ORIGINAL_REQUEST_URL_ATTR;

/**
 * WebFlux 工具类
 *
 * @author Lion Li
 */
public class WebFluxUtils {

    /**
     * 获取原请求路径（修复路径解析可能导致的数组越界问题）
     */
    public static String getOriginalRequestUrl(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        // 从网关属性中获取原始请求URL集合
        LinkedHashSet<URI> uris = exchange.getAttributeOrDefault(
            GATEWAY_ORIGINAL_REQUEST_URL_ATTR,
            new LinkedHashSet<>()
        );
        // 优先取集合中的第一个URL，若为空则取当前请求的URI
        URI requestUri = uris.stream().findFirst().orElse(request.getURI());
        // 安全处理：获取原始路径，为空时返回空字符串
        String rawPath = requestUri.getRawPath();
        if (StringUtils.isEmpty(rawPath)) {
            return "";
        }
        // 构建路径时增加空值校验，避免UriComponentsBuilder处理异常路径
        try {
            return UriComponentsBuilder.fromPath(rawPath).build().toUriString();
        } catch (Exception e) {
            // 异常路径直接返回原始字符串，避免解析失败
            return rawPath;
        }
    }

    /**
     * 是否是Json请求
     *
     * @param exchange HTTP请求
     */
    public static boolean isJsonRequest(ServerWebExchange exchange) {
        String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
        return StringUtils.startsWithIgnoreCase(header, MediaType.APPLICATION_JSON_VALUE);
    }

    /**
     * 读取request内的body
     *
     * 注意一个request只能读取一次 读取之后需要重新包装
     */
    public static String resolveBodyFromRequest(ServerHttpRequest serverHttpRequest) {
        // 获取请求体
        Flux<DataBuffer> body = serverHttpRequest.getBody();
        AtomicReference<String> bodyRef = new AtomicReference<>();
        body.subscribe(buffer -> {
            try (DataBuffer.ByteBufferIterator iterator = buffer.readableByteBuffers()) {
                // 安全处理：判断是否有可读缓冲区
                if (iterator.hasNext()) {
                    CharBuffer charBuffer = StandardCharsets.UTF_8.decode(iterator.next());
                    bodyRef.set(charBuffer.toString());
                } else {
                    bodyRef.set(""); // 无内容时返回空字符串，避免null
                }
                DataBufferUtils.release(buffer);
            } catch (Exception e) {
                bodyRef.set(""); // 异常时返回空字符串
            }
        });
        return bodyRef.get();
    }

    /**
     * 从缓存中读取request内的body
     *
     * 注意要求经过 {@link ServerWebExchangeUtils#cacheRequestBody(ServerWebExchange, Function)} 此方法创建缓存
     * 框架内已经使用 {@link WebCacheRequestFilter} 全局创建了body缓存
     *
     * @return body
     */
    public static String resolveBodyFromCacheRequest(ServerWebExchange exchange) {
        Object obj = exchange.getAttributes().get(ServerWebExchangeUtils.CACHED_REQUEST_BODY_ATTR);
        if (ObjectUtil.isNull(obj)) {
            return ""; // 缓存为空时返回空字符串，避免null
        }
        DataBuffer buffer = (DataBuffer) obj;
        try (DataBuffer.ByteBufferIterator iterator = buffer.readableByteBuffers()) {
            StringBuilder sb = new StringBuilder();
            // 遍历所有缓冲区，避免遗漏内容
            while (iterator.hasNext()) {
                sb.append(StandardCharsets.UTF_8.decode(iterator.next()));
            }
            return sb.toString();
        } catch (Exception e) {
            return ""; // 异常时返回空字符串，避免抛出异常
        }
    }

    /**
     * 设置webflux模型响应
     *
     * @param response ServerHttpResponse
     * @param value    响应内容
     * @return Mono<Void>
     */
    public static Mono<Void> webFluxResponseWriter(ServerHttpResponse response, Object value) {
        return webFluxResponseWriter(response, HttpStatus.OK, value, R.FAIL);
    }

    /**
     * 设置webflux模型响应
     *
     * @param response ServerHttpResponse
     * @param code     响应状态码
     * @param value    响应内容
     * @return Mono<Void>
     */
    public static Mono<Void> webFluxResponseWriter(ServerHttpResponse response, Object value, int code) {
        return webFluxResponseWriter(response, HttpStatus.OK, value, code);
    }

    /**
     * 设置webflux模型响应
     *
     * @param response ServerHttpResponse
     * @param status   http状态码
     * @param code     响应状态码
     * @param value    响应内容
     * @return Mono<Void>
     */
    public static Mono<Void> webFluxResponseWriter(ServerHttpResponse response, HttpStatus status, Object value, int code) {
        return webFluxResponseWriter(response, MediaType.APPLICATION_JSON_VALUE, status, value, code);
    }

    /**
     * 设置webflux模型响应
     *
     * @param response    ServerHttpResponse
     * @param contentType content-type
     * @param status      http状态码
     * @param code        响应状态码
     * @param value       响应内容
     * @return Mono<Void>
     */
    public static Mono<Void> webFluxResponseWriter(ServerHttpResponse response, String contentType, HttpStatus status, Object value, int code) {
        response.setStatusCode(status);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, contentType);
        // 安全处理：避免value为null导致JSON序列化异常
        String responseBody = JsonUtils.toJsonString(R.fail(code, ObjectUtil.defaultIfNull(value, "").toString()));
        DataBuffer dataBuffer = response.bufferFactory().wrap(responseBody.getBytes());
        return response.writeWith(Mono.just(dataBuffer));
    }
}
