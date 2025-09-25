package com.possable.service;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

import com.vaadin.flow.shared.Registration;

public class Broadcaster {

	private static final Set<Consumer<String>> listeners = new CopyOnWriteArraySet<>();

	public static Registration register(Consumer<String> listener) {
		listeners.add(listener);
		return () -> listeners.remove(listener);
	}

	public static void broadcast(String message) {
		for (Consumer<String> l : listeners) {
			try { l.accept(message); } catch (Exception ex) { /* ignore */ }
		}
	}
} 