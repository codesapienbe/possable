package com.possable.view;

import java.math.BigDecimal;
import java.util.List;

import com.possable.service.ItemService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public class ItemListComponent extends VerticalLayout {

	public ItemListComponent(ItemService itemService) {
		setPadding(false);
		setSpacing(false);
		setWidthFull();
		HorizontalLayout tiles = new HorizontalLayout();
		tiles.setWidthFull();
		tiles.getStyle().set("flex-wrap", "wrap").set("gap", "12px");
		List<ItemService.Item> items = itemService.listItems(200);
		for (var it : items) {
			Button b = new Button(it.name() + "\n" + String.format("$%.2f", it.price()));
			b.addClassName("pos-tile pos-tile-large");
			b.getElement().setAttribute("aria-label", it.name());
			tiles.add(b);
		}
		add(tiles);
	}
} 