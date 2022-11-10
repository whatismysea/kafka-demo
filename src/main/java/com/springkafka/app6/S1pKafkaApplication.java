package com.springkafka.app6;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.springkafka.Foo;
import com.springkafka.Bar;
import com.springkafka.CommonConfiguration;
import com.springkafka.ConfigProperties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.converter.StringJsonMessageConverter;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

@SpringBootApplication
@Import({ CommonConfiguration.class, ConfigProperties.class })
@EnableKafka
public class S1pKafkaApplication {

	public static void main(String[] args) throws Exception {
		ConfigurableApplicationContext context = new SpringApplicationBuilder(S1pKafkaApplication.class)
			.web(WebApplicationType.NONE)
			.run(args);
		TestBean testBean = context.getBean(TestBean.class);
		testBean.send(new GenericMessage<>(new Foo("foo", "bar")));
		context.getBean(Listener.class).latch.await(60, TimeUnit.SECONDS);
		context.close();
	}

	@Bean
	public TestBean test() {
		return new TestBean();
	}

	@Bean
	public Listener listener() {
		return new Listener();
	}

	@Bean
	public KafkaTemplate<String, String> kafkaTemplate(ProducerFactory<String, String> producerFactory,
			ConfigProperties config) {
		KafkaTemplate<String, String> kafkaTemplate = new KafkaTemplate<>(producerFactory);
		kafkaTemplate.setMessageConverter(new StringJsonMessageConverter());
		kafkaTemplate.setDefaultTopic(config.getFooTopic());
		return kafkaTemplate;
	}

	public static class TestBean {

		@Autowired
		private KafkaTemplate<String, ?> template;

		public void send(Message<?> foo) {
			this.template.send(foo);
		}

	}

	public static class Listener {

		private final CountDownLatch latch = new CountDownLatch(1);

		@KafkaListener(topics = "${kafka.fooTopic}", containerFactory = "jsonKafkaListenerContainerFactory")
		public void listen(Bar bar) {
			System.out.println("Received: " + bar);
			this.latch.countDown();
		}

	}

}
