import React from 'react';
import Header from '../../Header';
import Joi from 'joi-browser';
import Form from '../../../components/Form/Form';
import { uriSchemaCreate } from '../../../utils/endpoints';
import { toast } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';

class SchemaCreate extends Form {
  state = {
    formData: {
      subject: '',
      compatibilityLevel: 'BACKWARD',
      schemaData: '',
      schemaType: 'AVRO'
    },
    compatibilityLevelSelect: [
      { _id: 'NONE', name: 'NONE' },
      { _id: 'BACKWARD', name: 'BACKWARD' },
      { _id: 'BACKWARD_TRANSITIVE', name: 'BACKWARD_TRANSITIVE' },
      { _id: 'FORWARD', name: 'FORWARD' },
      { _id: 'FORWARD_TRANSITIVE', name: 'FORWARD_TRANSITIVE' },
      { _id: 'FULL', name: 'FULL' },
      { _id: 'FULL_TRANSITIVE', name: 'FULL_TRANSITIVE' }
    ],
    schemaTypeSelect: [
      { _id: 'AVRO', name: 'AVRO' },
      { _id: 'JSON', name: 'JSON' },
      { _id: 'PROTOBUF', name: 'PROTOBUF' }
    ],
    errors: {}
  };

  schema = {
    subject: Joi.string().required().label('subject'),
    compatibilityLevel: Joi.string().label('Compatibility').required(),
    schemaType: Joi.string().label('SchemaType').required(),
    schemaData: Joi.string().label('SchemaData').required()
  };

  onCleanupChange = value => {
    let { formData } = { ...this.state };
    formData.compatibilityLevel = value;

    this.setState({ formData });
  };

  async doSubmit() {
    const { formData } = this.state;
    const { clusterId } = this.props.match.params;

    const schemaType = formData.schemaType;
    let schemaData;
    let references;

    if (formData.schemaType === 'PROTOBUF') {
      schemaData = formData.schemaData;
    } else {
      const parsedSchemaData = JSON.parse(formData.schemaData);
      schemaData = parsedSchemaData.schema
        ? JSON.stringify(parsedSchemaData.schema)
        : formData.schemaData;
      references = parsedSchemaData.references || [];
    }

    const schema = {
      cluster: clusterId,
      subject: formData.subject,
      schema: schemaData,
      schemaType: schemaType,
      references: references,
      compatibilityLevel: formData.compatibilityLevel
    };

    this.postApi(uriSchemaCreate(clusterId), schema).then(() => {
      this.props.history.push({
        pathname: `/ui/${clusterId}/schema`
      });
      toast.success(`Schema '${formData.subject}' created`);
    });
  }

  render() {
    const { compatibilityLevelSelect, schemaTypeSelect, formData } = this.state;
    return (
      <div>
        <form
          encType="multipart/form-data"
          className="khq-form khq-form-config"
          onSubmit={() => this.doSubmit()}
        >
          <Header title="Create a schema" history={this.props.history} />
          {this.renderInput('subject', 'Subject', 'Subject')}

          {this.renderSelect(
            'compatibilityLevel',
            'Compatibility Level',
            compatibilityLevelSelect,
            value => {
              this.setState({ formData: { ...formData, compatibilityLevel: value.target.value } });
            },
            'col-sm-10'
          )}

          {this.renderSelect(
            'schemaType',
            'Schema Type',
            schemaTypeSelect,
            value => {
              this.setState({ formData: { ...formData, schemaType: value.target.value } });
            },
            'col-sm-10'
          )}

          {this.renderJSONInput('schemaData', 'Schema', value => {
            this.setState({
              formData: {
                ...formData,
                schemaData: value
              }
            });
          })}
          {this.renderButton(
            'Create',
            () => {
              this.doSubmit();
            },
            undefined,
            'button'
          )}
        </form>
      </div>
    );
  }
}

export default SchemaCreate;
