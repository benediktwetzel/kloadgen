package net.coru.kloadgen.config.kafkaheaders;

import static org.assertj.core.api.Assertions.assertThat;

import java.beans.PropertyDescriptor;
import java.util.Locale;
import org.apache.jmeter.util.JMeterUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class KafkaHeadersConfigElementBeanInfoTest {

  private static final String KAFKA_HEADERS = "kafkaHeaders";

  private KafkaHeadersConfigElementBeanInfo kafkaHeadersConfigElementBeanInfo;

  @BeforeEach
  public void setUp() {
    JMeterUtils.setLocale(Locale.ENGLISH);
    kafkaHeadersConfigElementBeanInfo = new KafkaHeadersConfigElementBeanInfo();
  }

  @Test
  public void shouldGenerateElements() {
    PropertyDescriptor[] propertyDescriptors = kafkaHeadersConfigElementBeanInfo.getPropertyDescriptors();
    assertThat(propertyDescriptors).hasSize(1);
    assertThat(propertyDescriptors[0].getName()).isEqualTo(KAFKA_HEADERS);
  }
}