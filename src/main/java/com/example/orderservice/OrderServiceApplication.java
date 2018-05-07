package com.example.orderservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;
import org.springframework.stereotype.Component;

@SpringBootApplication
public class OrderServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrderServiceApplication.class, args);
	}
}

@Component
class Runner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(Runner.class);

	private final StateMachineFactory<OrderStates, OrderEvents> factory;

	public Runner(StateMachineFactory<OrderStates, OrderEvents> factory) {
		this.factory = factory;
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {

		Long orderId = 132323L;
		StateMachine<OrderStates, OrderEvents> machine = this.factory.getStateMachine(Long.toString(orderId));
		machine.getExtendedState().getVariables().putIfAbsent("orderId", orderId);
		
		machine.start();
		log.info("Current state: " + machine.getState().getId().name());

		machine.sendEvent(OrderEvents.FULFILL);
		log.info("Current state: " + machine.getState().getId().name());

		machine.sendEvent(OrderEvents.PAY);
		log.info("Current state: " + machine.getState().getId().name());

		Message<OrderEvents> eventsMessage = MessageBuilder
												.withPayload(OrderEvents.FULFILL)
												.setHeader("a", "b")
												.build();
		machine.sendEvent(eventsMessage);
		log.info("Current state: " + machine.getState().getId().name());
	}

}

enum OrderEvents {
	FULFILL, PAY, CANCEL
}

enum OrderStates {
	SUBMITTED, PAID, FULFILLED, CANCELLED
}

/*
 * 3 Things we need to do: 1) The Engine itself 2) Overried the configure of
 * engine 3)
 * 
 * 
 * 
 */

// @Log
@Configuration
@EnableStateMachineFactory
class SimpleEnumStateMachineConfiguration extends StateMachineConfigurerAdapter<OrderStates, OrderEvents> {

	private static final Logger log = LoggerFactory.getLogger(SimpleEnumStateMachineConfiguration.class);

	@Override
	public void configure(StateMachineTransitionConfigurer<OrderStates, OrderEvents> transitions) throws Exception {
		transitions.withExternal()
				.source(OrderStates.SUBMITTED).target(OrderStates.PAID).event(OrderEvents.PAY)
				.and()
				.withExternal()
				.source(OrderStates.PAID).target(OrderStates.FULFILLED).event(OrderEvents.FULFILL)
				.and()
				.withExternal()
				.source(OrderStates.SUBMITTED).target(OrderStates.CANCELLED).event(OrderEvents.CANCEL)
				.and()
				.withExternal()
				.source(OrderStates.PAID).target(OrderStates.CANCELLED).event(OrderEvents.CANCEL)
				.and()
				.withExternal()
				.source(OrderStates.FULFILLED).target(OrderStates.CANCELLED).event(OrderEvents.CANCEL);
	}

	@Override
	public void configure(StateMachineStateConfigurer<OrderStates, OrderEvents> states) throws Exception {
		states.withStates()
				.initial(OrderStates.SUBMITTED)
				.stateEntry(OrderStates.SUBMITTED, new Action<OrderStates, OrderEvents>() {
					
					@Override
					public void execute(StateContext<OrderStates, OrderEvents> context) {
						Long orderId = Long.class.cast(context.getExtendedState().getVariables().getOrDefault("orderId", -1L));
						log.info("OrderId " + orderId + ".");
						log.info("Entering submitted state!");
					}
				})
				.state(OrderStates.PAID)
				.end(OrderStates.FULFILLED)
				.end(OrderStates.CANCELLED);
	}

	@Override
	public void configure(StateMachineConfigurationConfigurer<OrderStates, OrderEvents> config) throws Exception {

		StateMachineListenerAdapter<OrderStates, OrderEvents> adapter = new StateMachineListenerAdapter<OrderStates, OrderEvents>() {

			@Override
			public void stateChanged(State<OrderStates, OrderEvents> from, State<OrderStates, OrderEvents> to) {
				log.info(String.format("stateChanged(from %s to %s)", from + "", to + ""));
			}

		};

		config.withConfiguration().autoStartup(false).listener(adapter);
	}

}
