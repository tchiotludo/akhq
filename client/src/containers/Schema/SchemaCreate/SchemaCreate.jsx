import React from 'react';
import Header from '../../Header';
import Joi from 'joi-browser';
import Form from '../../../components/Form/Form';
import { post } from '../../../utils/api';
import { uriSchemaCreate } from '../../../utils/endpoints';
import { toast } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
class SchemaCreate extends Form {
  state = {
    formData: {
      subject: '',
      compatibilityLevel: 'BACKWARD',
      schemaData: ''
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
    errors: {}
  };

  schema = {
    subject: Joi.string()
      .required()
      .label('subject'),
    compatibilityLevel: Joi.string()
      .label('Compatibility')
      .required(),
    schemaData: Joi.string()
      .label('SchemaData')
      .required()
  };

  onCleanupChange = value => {
    let { formData } = { ...this.state };
    formData.compatibilityLevel = value;

    this.setState({ formData });
  };

  async doSubmit() {
    const { formData } = this.state;
    const { clusterId } = this.props.match.params;
    const schema = {
      cluster: clusterId,
      subject: formData.subject,
      schema: formData.schemaData,
      compatibilityLevel: formData.compatibilityLevel
    };

    post(uriSchemaCreate(clusterId), schema)
      .then(() => {
        this.props.history.push({
          pathname: `/ui/${clusterId}/schema`,
        });
        toast.success(`Schema '${formData.subject}' is created`);
      });
  }

  render() {
    const { compatibilityLevelSelect, formData } = this.state;
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
