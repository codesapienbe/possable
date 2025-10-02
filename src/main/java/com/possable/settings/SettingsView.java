package com.possable.settings;

import com.possable.infrastructure.ui.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "settings", layout = MainLayout.class)
@PageTitle("Settings")
public class SettingsView extends VerticalLayout {

	public SettingsView() {
		setWidthFull();
		setPadding(true);
		add(new H2("Settings"));
		add(new Paragraph("General application settings and theme toggle."));

		Button themeToggle = new Button("Toggle theme", e -> getUI().ifPresent(ui -> ui.getPage().executeJs(
			"const key='possable-theme';const cur=localStorage.getItem(key)||(document.body.classList.contains('light-mode')?'light':'dark');const next=cur==='light'?'dark':'light';if(next==='light'){document.body.classList.add('light-mode')}else{document.body.classList.remove('light-mode')}localStorage.setItem(key,next);"
		)));
		themeToggle.addClassName("pos-button-large");
		add(themeToggle);
	}
} 