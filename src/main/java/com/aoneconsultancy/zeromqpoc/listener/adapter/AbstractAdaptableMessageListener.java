package com.aoneconsultancy.zeromqpoc.listener.adapter;

import com.aoneconsultancy.zeromqpoc.core.MessageListener;
import com.aoneconsultancy.zeromqpoc.core.converter.MessageConverter;
import com.aoneconsultancy.zeromqpoc.core.converter.SimpleMessageConverter;
import com.aoneconsultancy.zeromqpoc.core.message.Message;
import com.aoneconsultancy.zeromqpoc.support.postprocessor.MessagePostProcessor;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.BeanResolver;
import org.springframework.expression.Expression;
import org.springframework.expression.ParserContext;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.support.StandardTypeConverter;
import org.springframework.util.Assert;
import org.zeromq.ZMQ;
import zmq.io.net.Address;
import zmq.io.net.NetProtocol;

/**
 * An abstract {@link MessageListener} adapter providing the
 * necessary infrastructure to extract the payload of a {@link Message}.
 */
@Slf4j
public abstract class AbstractAdaptableMessageListener implements MessageListener {

    private static final String DEFAULT_ENCODING = "UTF-8";

    private static final SpelExpressionParser PARSER = new SpelExpressionParser();

    private static final ParserContext PARSER_CONTEXT = new TemplateParserContext("!{", "}");

    private final StandardEvaluationContext evalContext = new StandardEvaluationContext();

    /**
     * -- SETTER --
     * Set the exchange to use when sending response messages.
     * This is only used if the exchange from the received message is null.
     * <p>
     * Response destinations are only relevant for listener methods
     * that return result objects, which will be wrapped in
     * a response message and sent to a response destination.
     *
     * @param responseExchange The exchange.
     */
    @Setter
    @Getter
    private ZMQ.Socket responseSocket = null;

    private Expression responseExpression;

    private boolean mandatoryPublish;

    @Setter
    @Getter
    private MessageConverter messageConverter = new SimpleMessageConverter();

    @Getter
    @Setter
    private String encoding = DEFAULT_ENCODING;

    private MessagePostProcessor[] beforeSendReplyPostProcessors;

    /**
     * Set post processors that will be applied before sending replies.
     *
     * @param beforeSendReplyPostProcessors the post processors.
     * @since 2.0.3
     */
    public void setBeforeSendReplyPostProcessors(MessagePostProcessor... beforeSendReplyPostProcessors) {
        Assert.noNullElements(beforeSendReplyPostProcessors, "'replyPostProcessors' must not have any null elements");
        this.beforeSendReplyPostProcessors = Arrays.copyOf(beforeSendReplyPostProcessors,
                beforeSendReplyPostProcessors.length);
    }

    /**
     * Set a bean resolver for runtime SpEL expressions. Also configures the evaluation
     * context with a standard type converter and map accessor.
     *
     * @param beanResolver the resolver.
     * @since 1.6
     */
    public void setBeanResolver(BeanResolver beanResolver) {
        this.evalContext.setBeanResolver(beanResolver);
        this.evalContext.setTypeConverter(new StandardTypeConverter());
        this.evalContext.addPropertyAccessor(new MapAccessor());
    }

    /**
     * Handle the given exception that arose during listener execution.
     * The default implementation logs the exception at error level.
     * <p>
     * Can be used by inheritors from overridden {@link #onMessage(Message)}
     *
     * @param ex the exception to handle
     */
    protected void handleListenerException(Throwable ex) {
        log.error("Listener execution failed", ex);
    }

    /**
     * Extract the message body from the given Rabbit message.
     *
     * @param message the Rabbit <code>Message</code>
     * @return the content of the message, to be passed into the listener method as argument
     */
    protected Object extractMessage(Message message) {
        MessageConverter converter = getMessageConverter();
        if (converter != null) {
            return converter.fromMessage(message);
        }
        return message;
    }

    /**
     * Handle the given result object returned from the listener method, sending a
     * response message back.
     *
     * @param resultArg the result object to handle (never <code>null</code>)
     * @param request   the original request message
     * @param socket    the ZeroMQ socket to operate on (may be <code>null</code>)
     */
    protected void handleResult(InvocationResult resultArg, Message request, ZMQ.Socket socket) {
        handleResult(resultArg, request, socket, null);
    }

    /**
     * Handle the given result object returned from the listener method, sending a
     * response message back.
     *
     * @param resultArg the result object to handle (never <code>null</code>)
     * @param request   the original request message
     * @param socket    the ZeroMQ socket to operate on (maybe <code>null</code>)
     * @param source    the source data for the method invocation - e.g.
     *                  {@code o.s.messaging.Message<?>}; may be null
     */
    protected void handleResult(InvocationResult resultArg, Message request, ZMQ.Socket socket, Object source) {
        if (socket != null) {
            if (resultArg.getReturnValue() instanceof CompletableFuture<?> completable) {
                log.debug("Sending ACK/NACK to the sender...");
                completable.whenComplete((r, t) -> {
                    if (t == null) {
                        sendResponse("Message Received and processed successfully.", request, socket);
                    } else {
                        sendResponse("Message Received but failed to process.", request, socket);
                    }
                });
            }
        } else if (log.isWarnEnabled()) {
            log.warn("Listener method returned result [" + resultArg
                    + "]: not generating response message for it because no ZMQ Socket given");
        }
    }

    private void sendResponse(String replyMsg, Message messageIn, ZMQ.Socket socket) {

        Message message = messageIn;
        if (this.beforeSendReplyPostProcessors != null) {
            for (MessagePostProcessor postProcessor : this.beforeSendReplyPostProcessors) {
                message = postProcessor.postProcessMessage(message);
            }
        }
        try {
            // TODO - getSender Socket address and reply to that
            socket.send(replyMsg, ZMQ.DONTWAIT);
        } catch (Exception e) {
            log.error("Failed to ack/nack message", e);
        }
    }

    /**
     * Convert to a message, with reply content type based on settings.
     *
     * @param result      the result.
     * @param genericType the type.
     * @param converter   the converter.
     * @return the message.
     * @since 2.3
     */
    protected Message convert(Object result, Type genericType, MessageConverter converter) {
        return converter.toMessage(result, Map.of(), genericType);
    }

    protected Address getReplyToAddress(Message request, Object source, InvocationResult result) {
        return new Address(NetProtocol.inproc, "localhost:5555");
    }

}
