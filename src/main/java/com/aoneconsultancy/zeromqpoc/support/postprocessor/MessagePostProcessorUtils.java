package com.aoneconsultancy.zeromqpoc.support.postprocessor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;

public final class MessagePostProcessorUtils {

    public static Collection<MessagePostProcessor> sort(Collection<MessagePostProcessor> processors) {
        List<MessagePostProcessor> priorityOrdered = new ArrayList<MessagePostProcessor>();
        List<MessagePostProcessor> ordered = new ArrayList<MessagePostProcessor>();
        List<MessagePostProcessor> unOrdered = new ArrayList<MessagePostProcessor>();
        for (MessagePostProcessor processor : processors) {
            if (processor instanceof PriorityOrdered) {
                priorityOrdered.add(processor);
            } else if (processor instanceof Ordered) {
                ordered.add(processor);
            } else {
                unOrdered.add(processor);
            }
        }
        OrderComparator.sort(priorityOrdered);
        List<MessagePostProcessor> sorted = new ArrayList<MessagePostProcessor>(priorityOrdered);
        OrderComparator.sort(ordered);
        sorted.addAll(ordered);
        sorted.addAll(unOrdered);
        return sorted;
    }

    private MessagePostProcessorUtils() {
    }

}
