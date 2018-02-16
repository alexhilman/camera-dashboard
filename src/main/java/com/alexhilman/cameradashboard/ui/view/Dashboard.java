package com.alexhilman.cameradashboard.ui.view;

import com.alexhilman.cameradashboard.ui.video.MovieFileManager;
import com.alexhilman.cameradashboard.ui.video.MovieViewHelper;
import com.google.inject.Inject;
import com.vaadin.guice.annotation.GuiceView;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewBeforeLeaveEvent;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.Sizeable;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 */
@GuiceView("")
public class Dashboard implements View {
    private static final Logger LOG = LogManager.getLogger(Dashboard.class);
    private static final NumberFormat NUMBER_FORMAT = new DecimalFormat("#,##0.0");

    private final MovieFileManager movieFileManager;
    private final MovieViewHelper movieViewHelper;
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    private VerticalLayout rootLayout;
    private ProgressBar storageBar;
    private Panel todayMovies;
    private ProgressBar memoryBar;

    @Inject
    public Dashboard(final MovieFileManager movieFileManager,
                     final MovieViewHelper movieViewHelper) {
        this.movieFileManager = movieFileManager;
        this.movieViewHelper = movieViewHelper;
    }

    public static String humanReadableByteCount(long bytes) {
        final int unit = 1000;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), "kMGTPE".charAt(exp - 1));
    }

    @Override
    public Component getViewComponent() {
        rootLayout = new VerticalLayout();
        rootLayout.setMargin(true);
        rootLayout.setSpacing(true);

        storageBar = new ProgressBar(0f);
        storageBar.setWidth(100, Sizeable.Unit.PERCENTAGE);
        rootLayout.addComponent(storageBar);

        memoryBar = new ProgressBar(0f);
        memoryBar.setWidth(100, Sizeable.Unit.PERCENTAGE);
        rootLayout.addComponent(memoryBar);

        todayMovies = new Panel("Today's Movies");
        todayMovies.setIcon(VaadinIcons.FILM);
        todayMovies.setSizeFull();
        rootLayout.addComponentsAndExpand(todayMovies);

        return rootLayout;
    }

    @Override
    public void beforeLeave(final ViewBeforeLeaveEvent event) {
//        executorService.shutdownNow();
    }

    @Override
    public void enter(final ViewChangeListener.ViewChangeEvent event) {
        final UI ui = UI.getCurrent();

        executorService.scheduleWithFixedDelay(
                () -> {
                    if (!ui.isAttached()) {
                        executorService.shutdownNow();
                        return;
                    }

                    final long usableSpace = movieFileManager.getUsableSpace();
                    final long totalSpace = movieFileManager.getTotalSpace();
                    final long usedSpace = totalSpace - usableSpace;
                    ui.access(() -> {
                        final float progress = (float) ((double) usedSpace / (double) totalSpace);
                        storageBar.setValue(progress);
                        storageBar.setCaption("Storage space: " + humanReadableByteCount(usedSpace) +
                                                      " used out of " + humanReadableByteCount(totalSpace) +
                                                      " (" + humanReadableByteCount(usableSpace) + " free)");
                    });
                },
                0,
                10,
                TimeUnit.SECONDS
        );

        executorService.scheduleWithFixedDelay(
                () -> {
                    if (!ui.isAttached()) {
                        executorService.shutdownNow();
                        return;
                    }

                    final Runtime runtime = Runtime.getRuntime();
                    ui.access(() -> {
                        final long usedMemory = runtime.totalMemory() - runtime.freeMemory();
                        final long totalMemory = runtime.maxMemory();
                        final double usedMemoryMB = usedMemory / 1024.0 / 1024.0;
                        memoryBar.setValue((float) usedMemory / (float) totalMemory);
                        memoryBar.setCaption("Used memory: " + NUMBER_FORMAT.format(usedMemoryMB) + "MB");
                    });
                },
                0,
                750,
                TimeUnit.MILLISECONDS);

        todayMovies.setContent(buildLoadingSpinner());
        Flowable.fromCallable(() -> movieFileManager.getMoviesInRange(midnightThisMorning(), Instant.now()))
                .map(movieViewHelper::buildPostersFor)
                .subscribeOn(Schedulers.computation())
                .subscribe(components -> {
                    ui.access(() -> {
                        if (components.length == 0) {
                            todayMovies.setContent(new Label("No videos for this day"));
                        } else {
                            final int columns = 3;
                            final int rows = components.length / columns + (components.length % columns > 0 ? 1 : 0);
                            final GridLayout content = new GridLayout(columns, rows, components);
                            content.setSpacing(true);
                            content.setMargin(true);
                            content.setWidth(100, Sizeable.Unit.PERCENTAGE);
                            todayMovies.setContent(content);
                        }
                        ui.push();
                    });
                });
    }

    private Component buildLoadingSpinner() {
        final HorizontalLayout layout = new HorizontalLayout();
        layout.setMargin(true);
        layout.setSpacing(true);
        layout.setSizeFull();

        final ProgressBar progressBar = new ProgressBar();
        progressBar.setIndeterminate(true);

        layout.addComponent(progressBar);
        layout.setComponentAlignment(progressBar, Alignment.MIDDLE_CENTER);

        return layout;
    }

    private Instant midnightThisMorning() {
        return Instant.now()
                      .atZone(ZoneId.systemDefault())
                      .withHour(0)
                      .withMinute(0)
                      .withSecond(0)
                      .withNano(0)
                      .toInstant();
    }
}
