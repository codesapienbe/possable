package com.possable.view;

import java.math.BigDecimal;
import java.util.List;

import com.possable.service.ItemService;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "items", layout = MainLayout.class)
@PageTitle("Items")
public class ItemListView extends VerticalLayout {

	public static record ItemDto(String id, String name, BigDecimal price, String category) {}

	private final Grid<ItemDto> grid = new Grid<>(ItemDto.class, false);

	public ItemListView(ItemService itemService) {
		setPadding(true);
		setSpacing(true);
		setWidthFull();

		add(new H1("Items"));

		grid.addColumn(ItemDto::name).setHeader("Name").setAutoWidth(true);
		grid.addColumn(item -> item.price().toString()).setHeader("Price").setAutoWidth(true);
		grid.addColumn(ItemDto::category).setHeader("Category").setAutoWidth(true);

		// Load from service (server-side) to keep credentials and auth on server
		grid.setItems(mapItems(itemService.listItems(100)));
		add(grid);
	}

	private List<ItemDto> mapItems(List<ItemService.Item> items) {
		return items.stream()
			.map(i -> new ItemDto(i.id(), i.name(), BigDecimal.valueOf(i.price()), i.description()))
			.toList();
	}
} 