package com.alexhilman.cameradashboard.ui.view;

import com.google.common.collect.Maps;
import com.vaadin.guice.annotation.GuiceView;
import com.vaadin.navigator.View;
import com.vaadin.ui.UI;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.concurrent.ConcurrentMap;

/**
 */
public class ClassNavigator {
    private static final ConcurrentMap<Class<? extends View>, String> viewNameByClass = Maps.newConcurrentMap();

    public static void navigateTo(final Class<? extends View> clazz) {
        UI.getCurrent().getNavigator().navigateTo(viewNameFromClass(clazz));
    }

    public static void navigateTo(final Class<? extends View> clazz, final String encodedParameters) {
        UI.getCurrent().getNavigator().navigateTo(viewNameFromClass(clazz) + "/" + encodedParameters);
    }

    private static String viewNameFromClass(final Class<? extends View> clazz) {
        String name = viewNameByClass.get(clazz);
        if (name == null) {
            name = reflectClassForVieName(clazz);
            viewNameByClass.putIfAbsent(clazz, name);
        }
        return name;
    }

    private static String reflectClassForVieName(final Class<? extends View> clazz) {
        final Annotation[] annotations = clazz.getAnnotations();
        return Arrays.stream(annotations)
                     .filter(annotation -> annotation.annotationType().equals(GuiceView.class))
                     .map(annotation -> (GuiceView) annotation)
                     .findFirst()
                     .map(GuiceView::value)
                     .orElseThrow(() -> new RuntimeException(clazz.getName() + " is not annotated with GuiceView"));
    }
}
