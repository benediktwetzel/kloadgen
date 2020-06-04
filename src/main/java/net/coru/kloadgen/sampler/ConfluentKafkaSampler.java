
package net.coru.kloadgen.sampler;

import static io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig.BASIC_AUTH_CREDENTIALS_SOURCE;
import static io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig.BEARER_AUTH_CREDENTIALS_SOURCE;
import static io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig.BEARER_AUTH_TOKEN_CONFIG;
import static io.confluent.kafka.schemaregistry.client.SchemaRegistryClientConfig.USER_INFO_CONFIG;
import static net.coru.kloadgen.util.ProducerKeysHelper.ACKS_CONFIG_DEFAULT;
import static net.coru.kloadgen.util.ProducerKeysHelper.BATCH_SIZE_CONFIG_DEFAULT;
import static net.coru.kloadgen.util.ProducerKeysHelper.BOOTSTRAP_SERVERS_CONFIG_DEFAULT;
import static net.coru.kloadgen.util.ProducerKeysHelper.BUFFER_MEMORY_CONFIG_DEFAULT;
import static net.coru.kloadgen.util.ProducerKeysHelper.COMPRESSION_TYPE_CONFIG_DEFAULT;
import static net.coru.kloadgen.util.ProducerKeysHelper.ENABLE_AUTO_SCHEMA_REGISTRATION_CONFIG;
import static net.coru.kloadgen.util.ProducerKeysHelper.ENABLE_AUTO_SCHEMA_REGISTRATION_CONFIG_DEFAULT;
import static net.coru.kloadgen.util.ProducerKeysHelper.FLAG_NO;
import static net.coru.kloadgen.util.ProducerKeysHelper.FLAG_YES;
import static net.coru.kloadgen.util.ProducerKeysHelper.JAAS_ENABLED;
import static net.coru.kloadgen.util.ProducerKeysHelper.JAVA_SEC_AUTH_LOGIN_CONFIG;
import static net.coru.kloadgen.util.ProducerKeysHelper.JAVA_SEC_AUTH_LOGIN_CONFIG_DEFAULT;
import static net.coru.kloadgen.util.ProducerKeysHelper.JAVA_SEC_KRB5_CONFIG;
import static net.coru.kloadgen.util.ProducerKeysHelper.JAVA_SEC_KRB5_CONFIG_DEFAULT;
import static net.coru.kloadgen.util.ProducerKeysHelper.KAFKA_HEADERS;
import static net.coru.kloadgen.util.ProducerKeysHelper.KAFKA_TOPIC_CONFIG;
import static net.coru.kloadgen.util.ProducerKeysHelper.KAFKA_TOPIC_CONFIG_DEFAULT;
import static net.coru.kloadgen.util.ProducerKeysHelper.KERBEROS_ENABLED;
import static net.coru.kloadgen.util.ProducerKeysHelper.LINGER_MS_CONFIG_DEFAULT;
import static net.coru.kloadgen.util.ProducerKeysHelper.RECEIVE_BUFFER_CONFIG_DEFAULT;
import static net.coru.kloadgen.util.ProducerKeysHelper.SAMPLE_ENTITY;
import static net.coru.kloadgen.util.ProducerKeysHelper.SASL_KERBEROS_SERVICE_NAME;
import static net.coru.kloadgen.util.ProducerKeysHelper.SASL_KERBEROS_SERVICE_NAME_DEFAULT;
import static net.coru.kloadgen.util.ProducerKeysHelper.SASL_MECHANISM;
import static net.coru.kloadgen.util.ProducerKeysHelper.SASL_MECHANISM_DEFAULT;
import static net.coru.kloadgen.util.ProducerKeysHelper.SEND_BUFFER_CONFIG_DEFAULT;
import static net.coru.kloadgen.util.ProducerKeysHelper.SSL_ENABLED;
import static net.coru.kloadgen.util.ProducerKeysHelper.TOPIC_NAME_STRATEGY;
import static net.coru.kloadgen.util.ProducerKeysHelper.VALUE_NAME_STRATEGY;
import static net.coru.kloadgen.util.ProducerKeysHelper.ZOOKEEPER_SERVERS;
import static net.coru.kloadgen.util.ProducerKeysHelper.ZOOKEEPER_SERVERS_DEFAULT;
import static net.coru.kloadgen.util.PropsKeysHelper.KEYED_MESSAGE_DEFAULT;
import static net.coru.kloadgen.util.PropsKeysHelper.KEYED_MESSAGE_KEY;
import static net.coru.kloadgen.util.PropsKeysHelper.MESSAGE_KEY_PLACEHOLDER_KEY;
import static net.coru.kloadgen.util.PropsKeysHelper.MESSAGE_VAL_PLACEHOLDER_KEY;
import static net.coru.kloadgen.util.PropsKeysHelper.MSG_KEY_PLACEHOLDER;
import static net.coru.kloadgen.util.PropsKeysHelper.MSG_PLACEHOLDER;
import static net.coru.kloadgen.util.SchemaRegistryKeyHelper.SCHEMA_REGISTRY_AUTH_BASIC_TYPE;
import static net.coru.kloadgen.util.SchemaRegistryKeyHelper.SCHEMA_REGISTRY_AUTH_BEARER_KEY;
import static net.coru.kloadgen.util.SchemaRegistryKeyHelper.SCHEMA_REGISTRY_AUTH_FLAG;
import static net.coru.kloadgen.util.SchemaRegistryKeyHelper.SCHEMA_REGISTRY_AUTH_KEY;
import static net.coru.kloadgen.util.SchemaRegistryKeyHelper.SCHEMA_REGISTRY_URL;
import static org.apache.kafka.common.config.SaslConfigs.SASL_JAAS_CONFIG;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import net.coru.kloadgen.exception.KLoadGenException;
import net.coru.kloadgen.model.HeaderMapping;
import net.coru.kloadgen.serializer.EnrichedRecord;
import net.coru.kloadgen.util.StatelessRandomTool;
import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.security.auth.SecurityProtocol;

@Slf4j
public class ConfluentKafkaSampler extends AbstractJavaSamplerClient implements Serializable {

    private static final long serialVersionUID = 1L;

    private KafkaProducer<String, Object> producer;

    private String topic;

    private String msg_key_placeHolder;

    private boolean key_message_flag = false;

    private final ObjectMapper objectMapperJson = new ObjectMapper();

    private transient StatelessRandomTool statelessRandomTool;

    @Override
    public Arguments getDefaultParameters() {

        Arguments defaultParameters = new Arguments();
        defaultParameters.addArgument(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS_CONFIG_DEFAULT);
        defaultParameters.addArgument(ZOOKEEPER_SERVERS, ZOOKEEPER_SERVERS_DEFAULT);
        defaultParameters.addArgument(KAFKA_TOPIC_CONFIG, KAFKA_TOPIC_CONFIG_DEFAULT);
        defaultParameters.addArgument(ProducerConfig.COMPRESSION_TYPE_CONFIG, COMPRESSION_TYPE_CONFIG_DEFAULT);
        defaultParameters.addArgument(ProducerConfig.BATCH_SIZE_CONFIG, BATCH_SIZE_CONFIG_DEFAULT);
        defaultParameters.addArgument(ProducerConfig.LINGER_MS_CONFIG, LINGER_MS_CONFIG_DEFAULT);
        defaultParameters.addArgument(ProducerConfig.BUFFER_MEMORY_CONFIG, BUFFER_MEMORY_CONFIG_DEFAULT);
        defaultParameters.addArgument(ProducerConfig.ACKS_CONFIG, ACKS_CONFIG_DEFAULT);
        defaultParameters.addArgument(ProducerConfig.SEND_BUFFER_CONFIG, SEND_BUFFER_CONFIG_DEFAULT);
        defaultParameters.addArgument(ProducerConfig.RECEIVE_BUFFER_CONFIG, RECEIVE_BUFFER_CONFIG_DEFAULT);
        defaultParameters.addArgument(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, SecurityProtocol.PLAINTEXT.name);
        defaultParameters.addArgument(KEYED_MESSAGE_KEY, KEYED_MESSAGE_DEFAULT);
        defaultParameters.addArgument(MESSAGE_KEY_PLACEHOLDER_KEY, MSG_KEY_PLACEHOLDER);
        defaultParameters.addArgument(MESSAGE_VAL_PLACEHOLDER_KEY, MSG_PLACEHOLDER);
        defaultParameters.addArgument(KERBEROS_ENABLED, FLAG_NO);
        defaultParameters.addArgument(JAAS_ENABLED, FLAG_NO);
        defaultParameters.addArgument(JAVA_SEC_AUTH_LOGIN_CONFIG, JAVA_SEC_AUTH_LOGIN_CONFIG_DEFAULT);
        defaultParameters.addArgument(JAVA_SEC_KRB5_CONFIG, JAVA_SEC_KRB5_CONFIG_DEFAULT);
        defaultParameters.addArgument(SASL_KERBEROS_SERVICE_NAME, SASL_KERBEROS_SERVICE_NAME_DEFAULT);
        defaultParameters.addArgument(SASL_MECHANISM, SASL_MECHANISM_DEFAULT);
        defaultParameters.addArgument(ENABLE_AUTO_SCHEMA_REGISTRATION_CONFIG, ENABLE_AUTO_SCHEMA_REGISTRATION_CONFIG_DEFAULT);
        defaultParameters.addArgument(VALUE_NAME_STRATEGY, TOPIC_NAME_STRATEGY);
        defaultParameters.addArgument(SSL_ENABLED, FLAG_NO);
        defaultParameters.addArgument(SslConfigs.SSL_KEY_PASSWORD_CONFIG, "<Key Password>");
        defaultParameters.addArgument(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, "<Keystore Location>");
        defaultParameters.addArgument(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, "<Keystore Password>");
        defaultParameters.addArgument(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, "<Truststore Location>");
        defaultParameters.addArgument(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, "<Truststore Password>");

        return defaultParameters;
    }

    @Override
    public void setupTest(JavaSamplerContext context) {

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, context.getParameter(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG));
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "io.confluent.kafka.serializers.KafkaAvroSerializer");
        props.put(ProducerConfig.ACKS_CONFIG, context.getParameter(ProducerConfig.ACKS_CONFIG));
        props.put(ProducerConfig.SEND_BUFFER_CONFIG, context.getParameter(ProducerConfig.SEND_BUFFER_CONFIG));
        props.put(ProducerConfig.RECEIVE_BUFFER_CONFIG, context.getParameter(ProducerConfig.RECEIVE_BUFFER_CONFIG));
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, context.getParameter(ProducerConfig.BATCH_SIZE_CONFIG));
        props.put(ProducerConfig.LINGER_MS_CONFIG, context.getParameter(ProducerConfig.LINGER_MS_CONFIG));
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, context.getParameter(ProducerConfig.BUFFER_MEMORY_CONFIG));
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, context.getParameter(ProducerConfig.COMPRESSION_TYPE_CONFIG));
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, context.getParameter(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG));
        props.put(SASL_MECHANISM, context.getParameter(SASL_MECHANISM));
        if (null != context.getParameter(VALUE_NAME_STRATEGY)) {
            props.put(VALUE_NAME_STRATEGY, context.getParameter(VALUE_NAME_STRATEGY));
        }

        Properties properties = JMeterContextService.getContext().getProperties();
        if (Objects.nonNull(properties.getProperty(SCHEMA_REGISTRY_URL))) {
            props.put(SCHEMA_REGISTRY_URL,
                properties.getProperty(SCHEMA_REGISTRY_URL));

            if (FLAG_YES.equals(properties.getProperty(SCHEMA_REGISTRY_AUTH_FLAG))) {
                if (SCHEMA_REGISTRY_AUTH_BASIC_TYPE
                    .equals(properties.getProperty(SCHEMA_REGISTRY_AUTH_KEY))) {
                    props.put(BASIC_AUTH_CREDENTIALS_SOURCE,
                        properties.getProperty(BASIC_AUTH_CREDENTIALS_SOURCE));
                    props.put(USER_INFO_CONFIG, properties.getProperty(USER_INFO_CONFIG));
                } else if (SCHEMA_REGISTRY_AUTH_BEARER_KEY.equals(properties.getProperty(SCHEMA_REGISTRY_AUTH_KEY))) {
                    props.put(BEARER_AUTH_CREDENTIALS_SOURCE,
                        properties.getProperty(BEARER_AUTH_CREDENTIALS_SOURCE));
                    props.put(BEARER_AUTH_TOKEN_CONFIG, properties.getProperty(BEARER_AUTH_TOKEN_CONFIG));
                }
            }
            props.put(ENABLE_AUTO_SCHEMA_REGISTRATION_CONFIG, context.getParameter(ENABLE_AUTO_SCHEMA_REGISTRATION_CONFIG));
        }
        Iterator<String> parameters = context.getParameterNamesIterator();
        parameters.forEachRemaining(parameter -> {
            if (parameter.startsWith("_")) {
                props.put(parameter.substring(1), context.getParameter(parameter));
            }
        });

        if (FLAG_YES.equalsIgnoreCase(context.getParameter(SSL_ENABLED))) {

            props.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, context.getParameter(SslConfigs.SSL_KEY_PASSWORD_CONFIG));
            props.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, context.getParameter(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG));
            props.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, context.getParameter(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG));
            props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, context.getParameter(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG));
            props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, context.getParameter(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG));
        }

        if (FLAG_YES.equalsIgnoreCase(context.getParameter(KERBEROS_ENABLED))) {
            System.setProperty(JAVA_SEC_AUTH_LOGIN_CONFIG, context.getParameter(JAVA_SEC_AUTH_LOGIN_CONFIG));
            System.setProperty(JAVA_SEC_KRB5_CONFIG, context.getParameter(JAVA_SEC_KRB5_CONFIG));
            props.put(SASL_KERBEROS_SERVICE_NAME, context.getParameter(SASL_KERBEROS_SERVICE_NAME));
        }

        if (FLAG_YES.equalsIgnoreCase(context.getParameter(JAAS_ENABLED))) {
            if (StringUtils.contains(context.getParameter(JAVA_SEC_AUTH_LOGIN_CONFIG), File.separatorChar)) {
                System.setProperty(JAVA_SEC_AUTH_LOGIN_CONFIG, context.getParameter(JAVA_SEC_AUTH_LOGIN_CONFIG));
            } else {
                props.put(SASL_JAAS_CONFIG, context.getParameter(JAVA_SEC_AUTH_LOGIN_CONFIG));
            }
        }

        if (FLAG_YES.equals(context.getParameter(KEYED_MESSAGE_KEY))) {
            key_message_flag= true;
            msg_key_placeHolder = context.getParameter(MESSAGE_KEY_PLACEHOLDER_KEY);//"Test-Key-String"; //UUID.randomUUID().toString();
        }

        topic = context.getParameter(KAFKA_TOPIC_CONFIG);
        producer = new KafkaProducer<>(props);
        statelessRandomTool = new StatelessRandomTool();

    }

    @Override
    public SampleResult runTest(JavaSamplerContext context) {

        SampleResult sampleResult = new SampleResult();
        sampleResult.sampleStart();
        JMeterContext jMeterContext = JMeterContextService.getContext();
        EnrichedRecord messageVal = (EnrichedRecord) jMeterContext.getVariables().getObject(SAMPLE_ENTITY);
        //noinspection unchecked
        List<HeaderMapping> kafkaHeaders = safeGetKafkaHeaders(jMeterContext);

        if (Objects.nonNull(messageVal)) {
            ProducerRecord<String, Object> producerRecord;
            try {
                if (key_message_flag) {
                    producerRecord = new ProducerRecord<>(topic, msg_key_placeHolder, messageVal.getGenericRecord());
                } else {
                    producerRecord = new ProducerRecord<>(topic, messageVal.getGenericRecord());
                }
                Map<String, String> jsonTypes = new HashMap<>();
                jsonTypes.put("contentType", "java.lang.String");
                producerRecord.headers()
                    .add("spring_json_header_types", objectMapperJson.writeValueAsBytes(jsonTypes));
                for (HeaderMapping kafkaHeader : kafkaHeaders) {
                    producerRecord.headers().add(kafkaHeader.getHeaderName(),
                        objectMapperJson.writeValueAsBytes(
                            statelessRandomTool
                                .generateRandom(kafkaHeader.getHeaderName(), kafkaHeader.getHeaderValue(), 10, Collections.emptyList())));
                }

                producer.send(producerRecord, (metadata, e) -> {
                    if (e != null) {
                        log.error("Send failed for record {}", producerRecord, e);
                        throw new KLoadGenException("Failed to sent message due ", e);
                    }
                });

                log.info("Send message to body: {}", producerRecord.value());

                sampleResult.setResponseData(messageVal.toString(), StandardCharsets.UTF_8.name());
                sampleResult.setSuccessful(true);
                sampleResult.sampleEnd();

            } catch (Exception e) {
                log.error("Failed to send message", e);
                sampleResult.setResponseData(e.getMessage(), StandardCharsets.UTF_8.name());
                sampleResult.setSuccessful(false);
                sampleResult.sampleEnd();
            }
        } else {
            log.error("Failed to Generate message");
            sampleResult.setResponseData("Failed to Generate message", StandardCharsets.UTF_8.name());
            sampleResult.setSuccessful(false);
            sampleResult.sampleEnd();
        }
        return sampleResult;
    }

    private List<HeaderMapping> safeGetKafkaHeaders(JMeterContext jMeterContext) {
        List<HeaderMapping> headerMappingList = new ArrayList<>();
        Object headers = jMeterContext.getSamplerContext().get(KAFKA_HEADERS);
        if (null != headers) {
            headerMappingList.addAll((List) headers);
        }
        return headerMappingList;
    }

    @Override
    public void teardownTest(JavaSamplerContext context) {
        producer.close();
    }
}
