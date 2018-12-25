package com.alexhilman.cameradashboard.ui.route;

import com.github.appreciated.app.layout.behaviour.AppLayout;
import com.github.appreciated.app.layout.behaviour.Behaviour;
import com.github.appreciated.app.layout.builder.AppLayoutBuilder;
import com.github.appreciated.app.layout.component.appmenu.left.LeftNavigationComponent;
import com.github.appreciated.app.layout.component.appmenu.left.builder.LeftAppMenuBuilder;
import com.github.appreciated.app.layout.design.AppLayoutDesign;
import com.github.appreciated.app.layout.router.AppLayoutRouterLayout;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.shared.ui.Transport;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
@Theme(value = Lumo.class, variant = Lumo.DARK)
@Push(value = PushMode.AUTOMATIC, transport = Transport.WEBSOCKET)
public class RootRouterLayout extends AppLayoutRouterLayout {
    private static final Logger LOG = LoggerFactory.getLogger(RootRouterLayout.class);

    @Override
    public AppLayout getAppLayout() {
        return AppLayoutBuilder
                .get(Behaviour.LEFT_RESPONSIVE_HYBRID)
                .withTitle("Security Dashboard")
                .withDesign(AppLayoutDesign.MATERIAL)
//                .withIcon("/logo.png")
                .withAppMenu(LeftAppMenuBuilder
                                     .get()
                                     .add(new LeftNavigationComponent("Dashboard",
                                                                      VaadinIcon.DASHBOARD.create(),
                                                                      DashboardRoute.class))
                                     .build())
                .build();
    }

}
